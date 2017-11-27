package de.gurkenlabs.litiengine.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class ImageCache.
 */
public final class ImageCache {
  /** The Constant CACHE_DIRECTORY. */
  public static final String CACHE_DIRECTORY = "cache/";

  public static final String CACHE_DUMP_NAME = "imagecache.dump";

  public static final ImageCache IMAGES = new ImageCache();
  public static final String IMAGES_DIRECTORY = "images";

  public static final String MAP_DIRECTORY = "map";

  public static final ImageCache MAPS = new ImageCache();

  public static final ImageCache SPRITES = new ImageCache();

  public static final String SPRITES_DIRECTORY = "sprites";

  /** The cache. */
  private final ConcurrentHashMap<String, BufferedImage> cache;

  /**
   * Instantiates a new image cache.
   *
   * @param subfolder
   *          the subfolder
   */
  private ImageCache() {
    this.cache = new ConcurrentHashMap<>();
  }

  public void clear() {
    this.cache.clear();
  }

  public void clear(final String regex) {
    List<String> remove = new ArrayList<>();
    for (String key : this.cache.keySet()) {
      if (key.matches(regex)) {
        remove.add(key);
      }
    }

    for (String key : remove) {
      this.cache.remove(key);
    }
  }

  /**
   * Contains key.
   *
   * @param key
   *          the key
   * @return true, if successful
   */
  public boolean containsKey(final String key) {
    return this.cache.containsKey(key);
  }

  /**
   * Gets the.
   *
   * @param key
   *          the key
   * @return the buffered image
   */
  public BufferedImage get(final String key) {
    if (this.cache.containsKey(key)) {
      return this.cache.get(key);
    }

    return null;
  }

  /**
   * Put.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   * @return the buffered image
   */
  public BufferedImage put(final String key, final BufferedImage value) {
    return this.cache.put(key, value);
  }

  /**
   * Put persistent.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   * @return the buffered image
   */
  public BufferedImage putPersistent(final String key, final BufferedImage value) {
    if (key == null || key.isEmpty() || value == null) {
      return null;
    }

    this.cache.put(key, value);
    return value;
  }
}
