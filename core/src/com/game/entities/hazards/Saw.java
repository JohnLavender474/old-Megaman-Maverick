package com.game.entities.hazards;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.GameContext2d;
import com.game.animations.AnimationComponent;
import com.game.animations.TimedAnimation;
import com.game.entities.Entity;
import com.game.movement.PendulumComponent;
import com.game.movement.RotatingLineComponent;
import com.game.movement.TrajectoryComponent;
import com.game.shapes.LineComponent;
import com.game.shapes.LineHandle;
import com.game.shapes.ShapeComponent;
import com.game.shapes.ShapeHandle;
import com.game.sprites.SpriteComponent;
import com.game.sprites.SpriteProcessor;
import com.game.utils.enums.Position;
import com.game.utils.interfaces.UpdatableConsumer;
import com.game.utils.objects.Pendulum;
import com.game.utils.objects.RotatingLine;
import com.game.utils.objects.Wrapper;
import com.game.world.BodyComponent;
import com.game.world.Fixture;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.graphics.Color.DARK_GRAY;
import static com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled;
import static com.game.ViewVals.PPM;
import static com.game.assets.TextureAsset.HAZARDS_1;
import static com.game.utils.UtilMethods.*;
import static com.game.utils.enums.Position.CENTER;
import static com.game.utils.objects.Pair.pairOf;
import static com.game.world.BodyType.ABSTRACT;
import static com.game.world.FixtureType.DEATH;
import static com.game.world.FixtureType.SHIELD;

public class Saw extends Entity {

    private static final float LENGTH = 3f;
    private static final float ROTATION_SPEED = 2f;
    private static final float PENDULUM_SCALAR = 1f;

    public Saw(GameContext2d gameContext, Rectangle rectangle) {
        this(gameContext, centerPoint(rectangle));
    }

    public Saw(GameContext2d gameContext, float centerX, float centerY) {
        this(gameContext, new Vector2(centerX, centerY));
    }

    public Saw(GameContext2d gameContext, Vector2 center) {
        super(gameContext);
        addComponent(animationComponent(gameContext));
        addComponent(bodyComponent(center));
        addComponent(spriteComponent());
    }

    public Saw(GameContext2d gameContext, RectangleMapObject sawObj) {
        this(gameContext, sawObj.getRectangle());
        MapProperties properties = sawObj.getProperties();
        Rectangle rect = sawObj.getRectangle();
        if (properties.containsKey("p")) {
            setToPendulum(rect);
        } else if (properties.containsKey("r")) {
            setToRotation(rect);
        } else if (properties.containsKey("t")) {
            setToTrajectory(properties);
        }
    }

    private void setToPendulum(Rectangle rect) {
        Pendulum pendulum = new Pendulum(LENGTH * PPM, 10f * PPM, bottomCenterPoint(rect), PENDULUM_SCALAR);
        UpdatableConsumer<Pendulum> updatableConsumer = (pendulum1, delta) ->
                getComponent(BodyComponent.class).setCenter(pendulum1.getEnd());
        addComponent(new PendulumComponent(pendulum, updatableConsumer));
        LineHandle lineHandle = new LineHandle();
        lineHandle.setLineSupplier(() -> pairOf(pendulum.getAnchor(), pendulum.getEnd()));
        lineHandle.setColorSupplier(() -> DARK_GRAY);
        lineHandle.setThicknessSupplier(() -> PPM / 8f);
        lineHandle.setShapeTypeSupplier(() -> Filled);
        addComponent(new LineComponent(lineHandle));
        Circle circle1 = new Circle(pendulum.getAnchor(), PPM / 4f);
        Circle circle2 = new Circle();
        circle2.setRadius(PPM / 4f);
        List<ShapeHandle> shapeHandles = new ArrayList<>();
        ShapeHandle shapeHandle1 = new ShapeHandle();
        shapeHandle1.setShapeSupplier(() -> circle1);
        shapeHandle1.setShapeTypeSupplier(() -> Filled);
        shapeHandle1.setColorSupplier(() -> DARK_GRAY);
        shapeHandles.add(shapeHandle1);
        ShapeHandle shapeHandle2 = new ShapeHandle();
        shapeHandle2.copyOf(shapeHandle1);
        shapeHandle2.setShapeSupplier(() -> circle2);
        shapeHandle2.setUpdatable(delta -> circle2.setPosition(pendulum.getEnd()));
        shapeHandles.add(shapeHandle2);
        addComponent(new ShapeComponent(shapeHandles));
    }

    private void setToRotation(Rectangle rect) {
        RotatingLine rotatingLine = new RotatingLine(centerPoint(rect), LENGTH * PPM, ROTATION_SPEED * PPM);
        UpdatableConsumer<RotatingLine> updatableConsumer = (rotatingLine1, delta) ->
                getComponent(BodyComponent.class).setCenter(rotatingLine1.getEndPoint());
        addComponent(new RotatingLineComponent(rotatingLine, updatableConsumer));
        LineHandle lineHandle = new LineHandle();
        lineHandle.setLineSupplier(() -> pairOf(rotatingLine.getPos(), rotatingLine.getEndPoint()));
        lineHandle.setColorSupplier(() -> DARK_GRAY);
        lineHandle.setThicknessSupplier(() -> PPM / 8f);
        lineHandle.setShapeTypeSupplier(() -> Filled);
        addComponent(new LineComponent(lineHandle));
        Circle circle1 = new Circle();
        circle1.setRadius(PPM / 4f);
        Circle circle2 = new Circle();
        circle2.setRadius(PPM / 4f);
        List<ShapeHandle> shapeHandles = new ArrayList<>();
        ShapeHandle shapeHandle1 = new ShapeHandle();
        shapeHandle1.setShapeSupplier(() -> circle1);
        shapeHandle1.setShapeTypeSupplier(() -> Filled);
        shapeHandle1.setColorSupplier(() -> DARK_GRAY);
        shapeHandle1.setUpdatable(delta -> circle1.setPosition(rotatingLine.getPos()));
        shapeHandles.add(shapeHandle1);
        ShapeHandle shapeHandle2 = new ShapeHandle();
        shapeHandle2.copyOf(shapeHandle1);
        shapeHandle2.setShapeSupplier(() -> circle2);
        shapeHandle2.setUpdatable(delta -> circle2.setPosition(rotatingLine.getEndPoint()));
        shapeHandles.add(shapeHandle2);
        addComponent(new ShapeComponent(shapeHandles));
    }

    private void setToTrajectory(MapProperties properties) {
        String trajStr = properties.get("trajectory", String.class);
        TrajectoryComponent trajectoryComponent = new TrajectoryComponent(trajStr,
                centerPoint(getComponent(BodyComponent.class).getCollisionBox()));
        addComponent(trajectoryComponent);
    }

    private SpriteComponent spriteComponent() {
        Sprite sprite = new Sprite();
        sprite.setSize(2f * PPM, 2f * PPM);
        return new SpriteComponent(sprite, new SpriteProcessor() {

            @Override
            public int getSpriteRenderPriority() {
                return 10;
            }

            @Override
            public boolean setPositioning(Wrapper<Rectangle> bounds, Wrapper<Position> position) {
                bounds.setData(getComponent(BodyComponent.class).getCollisionBox());
                position.setData(CENTER);
                return true;
            }
        });
    }

    private AnimationComponent animationComponent(GameContext2d gameContext) {
        TextureRegion textureRegion = gameContext.getAsset(HAZARDS_1.getSrc(), TextureAtlas.class)
                .findRegion("Saw");
        TimedAnimation timedAnimation = new TimedAnimation(textureRegion, 2, .1f);
        return new AnimationComponent(timedAnimation);
    }

    private BodyComponent bodyComponent(Vector2 center) {
        BodyComponent bodyComponent = new BodyComponent(ABSTRACT);
        bodyComponent.setSize(2f * PPM, 2f * PPM);
        bodyComponent.setCenter(center);
        // Death 1
        Rectangle death1 = new Rectangle();
        death1.setSize(2f * PPM, PPM);
        death1.setCenter(center);
        bodyComponent.addFixture(new Fixture(this, death1, DEATH));
        // Death 2
        Rectangle death2 = new Rectangle();
        death2.setSize(PPM, 2f * PPM);
        death2.setCenter(center);
        bodyComponent.addFixture(new Fixture(this, death2, DEATH));
        // Shield 1
        Rectangle shield1 = new Rectangle();
        shield1.setSize(PPM, PPM);
        setBottomCenterToPoint(shield1, bodyComponent.getCenter());
        Fixture shield1Fixture = new Fixture(this, shield1, SHIELD);
        shield1Fixture.putUserData("reflectDir", "up");
        bodyComponent.addFixture(shield1Fixture);
        // Shield 2
        Rectangle shield2 = new Rectangle();
        shield2.setSize(PPM, PPM);
        setTopCenterToPoint(shield2, bodyComponent.getCenter());
        Fixture shield2Fixture = new Fixture(this, shield2, SHIELD);
        shield2Fixture.putUserData("reflectDir", "down");
        bodyComponent.addFixture(shield2Fixture);
        return bodyComponent;
    }

}
