package de.gurkenlabs.litiengine.physics;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import de.gurkenlabs.litiengine.entities.IEntityController;
import de.gurkenlabs.litiengine.entities.IMobileEntity;

public interface IMovementController extends IEntityController {

  public float getDx();

  public void setDx(float dx);

  public float getDy();

  public void setDy(float dy);

  public void apply(Force force);

  public List<Force> getActiveForces();

  public void onMovementCheck(Predicate<IMobileEntity> predicate);

  public void onMoved(Consumer<Point2D> cons);
}
