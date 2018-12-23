package de.gurkenlabs.litiengine.environment.tilemap;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.graphics.ImageRenderer;
import de.gurkenlabs.litiengine.graphics.RenderType;
import de.gurkenlabs.litiengine.graphics.Spritesheet;
import de.gurkenlabs.litiengine.resources.Resources;
import de.gurkenlabs.litiengine.util.ImageProcessing;

public class HexagonalMapRenderer implements IMapRenderer {
  @Override
  public BufferedImage getImage(IMap map, RenderType... renderTypes) {
    final String cacheKey = getCacheKey(map) + "_" + renderTypes;
    Optional<BufferedImage> opt = Resources.images().tryGet(cacheKey);
    if (opt.isPresent()) {
      return opt.get();
    }

    final BufferedImage img = ImageProcessing.getCompatibleImage((int) map.getSizeInPixels().getWidth(), (int) map.getSizeInPixels().getHeight());
    final Graphics2D g = img.createGraphics();

    for (final ITileLayer layer : map.getTileLayers()) {
      if (layer == null || !shouldBeRendered(layer, renderTypes)) {
        continue;
      }

      ImageRenderer.render(g, this.getLayerImage(layer, map, true), layer.getOffset());
    }

    g.dispose();

    Resources.images().add(cacheKey, img);
    return img;
  }

  @Override
  public BufferedImage getImage(IMap map) {
    return this.getImage(map, RenderType.BACKGROUND, RenderType.GROUND, RenderType.SURFACE, RenderType.NORMAL, RenderType.OVERLAY);
  }

  @Override
  public MapOrientation getSupportedOrientation() {
    return MapOrientation.HEXAGONAL;
  }

  @Override
  public void render(final Graphics2D g, final IMap map, RenderType... renderTypes) {
    this.render(g, map, 0, 0, renderTypes);
  }

  @Override
  public void render(final Graphics2D g, final IMap map, final double offsetX, final double offsetY, RenderType... renderTypes) {
    final BufferedImage mapImage = this.getImage(map, renderTypes);
    ImageRenderer.render(g, mapImage, offsetX, offsetY);
  }

  @Override
  public void render(final Graphics2D g, final IMap map, final Rectangle2D viewport, RenderType... renderTypes) {
    for (final ILayer layer : map.getRenderLayers()) {
      if (layer == null || !shouldBeRendered(layer, renderTypes)) {
        continue;
      }

      if (layer instanceof ITileLayer) {
        this.renderTileLayerImage(g, (ITileLayer) layer, map, viewport);
      }

      if (layer instanceof IImageLayer) {
        this.renderImageLayer(g, (IImageLayer) layer, viewport);
      }
    }
  }

  private static boolean shouldBeRendered(ILayer layer, RenderType[] renderTypes) {
    if (renderTypes == null || renderTypes.length == 0) {
      return isVisible(layer);
    }

    for (RenderType alloc : renderTypes) {
      if (alloc == layer.getRenderType()) {
        return isVisible(layer);
      }
    }

    return false;
  }

  private static boolean isVisible(ILayer layer) {
    return layer.isVisible() && layer.getOpacity() > 0;
  }

  /**
   * Gets the cache key.
   *
   * @param map
   *          the map
   * @return the cache key
   */
  private static String getCacheKey(final IMap map) {
    return "map_" + map.getName();
  }

  private static Image getTileImage(final IMap map, final ITile tile) {
    if (tile == null) {
      return null;
    }

    final ITileset tileset = MapUtilities.findTileSet(map, tile);
    if (tileset == null || tileset.getFirstGridId() > tile.getGridId()) {
      return null;
    }

    Spritesheet sprite = tileset.getSpritesheet();
    if (sprite == null) {
      return null;
    }

    // get the grid id relative to the sprite sheet since we use a 0 based
    // approach to calculate the position
    int index = tile.getGridId() - tileset.getFirstGridId();

    // support for animated tiles
    final ITileAnimation animation = MapUtilities.getAnimation(map, index);
    if (animation != null && !animation.getFrames().isEmpty()) {
      final long playedMs = Game.time().sinceGameStart();

      final int totalDuration = animation.getTotalDuration();
      final long animationsPlayed = playedMs / totalDuration;

      final long deltaTicks = playedMs - animationsPlayed * totalDuration;
      int currentPlayTime = 0;
      for (final ITileAnimationFrame frame : animation.getFrames()) {
        currentPlayTime += frame.getDuration();
        if (deltaTicks < currentPlayTime) {
          // found the current animation frame
          index = frame.getTileId();
          break;
        }
      }
    }

    BufferedImage tileImage = sprite.getSprite(index, tileset.getMargin(), tileset.getSpacing());
    if (tile.isFlipped()) {
      if (tile.isFlippedDiagonally()) {
        tileImage = ImageProcessing.rotate(tileImage, Math.toRadians(90));
        tileImage = ImageProcessing.verticalFlip(tileImage);
      }

      if (tile.isFlippedHorizontally()) {
        tileImage = ImageProcessing.horizontalFlip(tileImage);
      }

      if (tile.isFlippedVertically()) {
        tileImage = ImageProcessing.verticalFlip(tileImage);
      }
    }

    return tileImage;
  }

  /**
   * Gets the layer image.
   *
   * @param layer
   *          the layer
   * @param map
   *          the map
   * @return the layer image
   */
  private synchronized BufferedImage getLayerImage(final ITileLayer layer, final IMap map, boolean includeAnimationTiles) {
    // if we have already retrived the image, use the one from the cache to
    // draw the layer
    final String cacheKey = getCacheKey(map) + "_" + layer.getName();
    Optional<BufferedImage> opt = Resources.images().tryGet(cacheKey);
    if (opt.isPresent()) {
      return opt.get();
    }

    final BufferedImage bufferedImage = ImageProcessing.getCompatibleImage(map.getSizeInPixels().width, map.getSizeInPixels().height);

    // we need a graphics 2D object to work with transparency
    final Graphics2D imageGraphics = bufferedImage.createGraphics();

    // set alpha value of the tiles by the layers value
    final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.getOpacity());
    imageGraphics.setComposite(ac);

    for (int y = 0; y < layer.getSizeInTiles().height; y++) {
      for (int x = 0; x < layer.getSizeInTiles().width; x++) {
        ITile tile = layer.getTile(x, y);
        Rectangle tileBounds = map.getTileShape(x, y).getBounds();
        if (tile == null || (!includeAnimationTiles && MapUtilities.hasAnimation(map, tile))) {
          continue;
        }

        final Image tileTexture = getTileImage(map, tile);
        //There are two offset properties: TileOffset from the TileSet and layer offset.
        int tileOffsetX = 0;
        int tileOffsetY = 0;
        final ITileOffset tileOffset = MapUtilities.findTileSet(map, tile).getTileOffset();
        if (tileOffset != null) {
          tileOffsetX = tileOffset.getX();
          tileOffsetY = tileOffset.getY();
        }

        tileOffsetX -= MapUtilities.findTileSet(map, tile).getTileWidth() - map.getTileWidth();
        tileOffsetY -= MapUtilities.findTileSet(map, tile).getTileHeight() - map.getTileHeight();

        final double offsetX = layer.getOffset().x;
        final double offsetY = layer.getOffset().y;

        ImageRenderer.render(imageGraphics, tileTexture, offsetX + tileBounds.getX() + tileOffsetX, offsetY + tileBounds.getY() + tileOffsetY);
      }
    }

    Resources.images().add(cacheKey, bufferedImage);
    return bufferedImage;
  }

  /**
   * Renders the tiles from the specified layer that lie within the bounds of
   * the viewport. This rendering of static tiles is cached when when the
   * related graphics setting is enabled, which tremendously improves the
   * rendering performance.
   *
   * @param g
   * @param layer
   * @param map
   * @param viewport
   */
  private void renderTileLayerImage(final Graphics2D g, final ITileLayer layer, final IMap map, final Rectangle2D viewport) {
    // set alpha value of the tiles by the layers value
    final Composite oldComp = g.getComposite();
    final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.getOpacity());
    g.setComposite(ac);

    for (int x = 0; x < map.getWidth(); x++) {
      for (int y = 0; y < map.getHeight(); y++) {
        ITile tile = layer.getTile(x, y);
        Rectangle2D tileBounds = map.getTileShape(x, y).getBounds2D();
        if (tile == null || !viewport.intersects(tileBounds)) {
          continue;
        }
        final Image tileTexture = getTileImage(map, tile);

        //There are two offset properties: TileOffset from the TileSet and layer offset.
        int tileOffsetX = 0;
        int tileOffsetY = 0;
        final ITileOffset tileOffset = MapUtilities.findTileSet(map, tile).getTileOffset();
        if (tileOffset != null) {
          tileOffsetX = tileOffset.getX();
          tileOffsetY = tileOffset.getY();
        }

        tileOffsetX -= MapUtilities.findTileSet(map, tile).getTileWidth() - map.getTileWidth();
        tileOffsetY -= MapUtilities.findTileSet(map, tile).getTileHeight() - map.getTileHeight();

        final double offsetX = -(viewport.getX()) + layer.getOffset().x;
        final double offsetY = -(viewport.getY()) + layer.getOffset().y;

        ImageRenderer.render(g, tileTexture, offsetX + tileBounds.getX() + tileOffsetX, offsetY + tileBounds.getY() + tileOffsetY);
      }
    }

    g.setComposite(oldComp);
  }

  private void renderImageLayer(Graphics2D g, IImageLayer layer, Rectangle2D viewport) {
    Spritesheet sprite = Resources.spritesheets().get(layer.getImage().getSource());
    if (sprite == null) {
      return;
    }

    final Composite oldComp = g.getComposite();
    final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.getOpacity());
    g.setComposite(ac);

    final double viewportOffsetX = -viewport.getX() + layer.getOffset().x;
    final double viewportOffsetY = -viewport.getY() + layer.getOffset().y;

    ImageRenderer.render(g, sprite.getImage(), viewportOffsetX, viewportOffsetY);
    g.setComposite(oldComp);
  }
}