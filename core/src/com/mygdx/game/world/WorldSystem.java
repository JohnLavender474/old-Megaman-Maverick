package com.mygdx.game.world;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.core.Component;
import com.mygdx.game.core.System;
import com.mygdx.game.core.Entity;
import com.mygdx.game.utils.UtilMethods;
import com.mygdx.game.utils.exceptions.InvalidFieldException;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link System} implementation that handles the logic of the "game world physics", i.e. gravity, collision handling,
 * and contact-event-handling.
 */
@RequiredArgsConstructor
public class WorldSystem extends System {

    private final Set<Contact> currentContacts = new HashSet<>();
    private final Set<Contact> priorContacts = new HashSet<>();
    private final ContactListener contactListener;
    private final float fixedTimeStep;
    private float accumulator;

    @Override
    public Set<Class<? extends Component>> getComponentMask() {
        return Set.of(BodyComponent.class);
    }

    @Override
    public void update(float delta) {
        accumulator += delta;
        while (accumulator >= fixedTimeStep) {
            accumulator -= fixedTimeStep;
            super.update(fixedTimeStep);
        }
    }

    @Override
    protected void processEntity(Entity entity, float delta) {
        BodyComponent bodyComponent = entity.getComponent(BodyComponent.class);
        // Check that friction scalars are correct, throw exception if any are invalid
        Vector2 frictionScalar = bodyComponent.getFrictionScalarCopy();
        if (frictionScalar.x > 1f || frictionScalar.x < 0f) {
            throw new InvalidFieldException(String.valueOf(frictionScalar.x),
                                            "friction scalar x", "body component of " + entity);
        }
        if (frictionScalar.y > 1f || frictionScalar.y < 0f) {
            throw new InvalidFieldException(String.valueOf(frictionScalar.x),
                                            "friction scalar y", "body component of " + entity);
        }
        // Apply friction-scaled velocity to body
        float x = bodyComponent.getVelocity().x * frictionScalar.x;
        float y = bodyComponent.getVelocity().y * frictionScalar.y;
        // Apply friction-scaled impulse to body
        x += bodyComponent.getImpulse().x * frictionScalar.x;
        y += bodyComponent.getImpulse().y * frictionScalar.y;
        // Apply friction-scaled gravity to body
        x += bodyComponent.getGravity().x * frictionScalar.x;
        y += bodyComponent.getGravity().y * frictionScalar.y;
        // Scale x and y to delta and add values to Body Component collision box,
        // important to note that delta == fixedTimeStep
        bodyComponent.getCollisionBox().x += x * delta;
        bodyComponent.getCollisionBox().y += y * delta;
        // The Entity is moved to conform to the Body Component's specified position on the Entity
        UtilMethods.positionRectOntoOther(entity.getBoundingBox(), bodyComponent.getCollisionBox(),
                                          bodyComponent.getPositionOnEntity());
        // Each Fixture is moved to conform to its position offset from the center of the Body Component
        bodyComponent.getFixtures().forEach(fixture -> {
            Vector2 center = new Vector2();
            bodyComponent.getCollisionBox().getCenter(center);
            float fixtureX = center.x + fixture.getOffset().x;
            float fixtureY = center.y + fixture.getOffset().y;
            fixture.getFixtureBox().setCenter(fixtureX, fixtureY);
        });
    }

    @Override
    protected void postProcess(float delta) {
        // Fetch all Body Components
        Set<BodyComponent> bodyComponents = getUnmodifiableCopyOfListOfEntities()
                .stream().map(entity -> entity.getComponent(BodyComponent.class))
                .collect(Collectors.toSet());
        // Reset impulse to zero
        bodyComponents.forEach(bodyComponent -> bodyComponent.getImpulse().setZero());
        // Handle contacts
        Iterator<BodyComponent> bodyComponentIterator = bodyComponents.iterator();
        while (bodyComponentIterator.hasNext()) {
            BodyComponent bodyComponent = bodyComponentIterator.next();
            for (BodyComponent otherBC : bodyComponents) {
                if (bodyComponent.equals(otherBC)) {
                    continue;
                }
                Rectangle overlap = new Rectangle();
                if (Intersector.intersectRectangles(bodyComponent.getCollisionBox(),
                                                    otherBC.getCollisionBox(), overlap)) {
                    handleCollision(bodyComponent, otherBC, overlap);
                }
                for (Fixture f1 : bodyComponent.getFixtures()) {
                    for (Fixture f2 : otherBC.getFixtures()) {
                        if (Intersector.overlaps(f1.getFixtureBox(), f2.getFixtureBox())) {
                            currentContacts.add(new Contact(f1, f2));
                        }
                    }
                }
            }
            // Remove body from set
            bodyComponentIterator.remove();
        }
        // Handles Contact instances in the current contacts set
        currentContacts.forEach(currentContact -> {
            if (!priorContacts.contains(currentContact)) {
                contactListener.beginContact(currentContact, delta);
            }
        });
        // Handles Contact instances in the prior contacts set
        priorContacts.forEach(priorContact -> {
            if (!currentContacts.contains(priorContact)) {
                contactListener.endContact(priorContact, delta);
            }
        });
        // Moves current contacts to prior, then clears the set of current contacts
        priorContacts.clear();
        priorContacts.addAll(currentContacts);
        currentContacts.clear();
    }

    private void handleCollision(BodyComponent bc1, BodyComponent bc2, Rectangle overlap) {
        if (overlap.getWidth() > overlap.getHeight()) {
            if (bc1.getCollisionBox().getY() > bc2.getCollisionBox().getY()) {
                bc1.getCollisionBox().y += overlap.getHeight();
            } else {
                bc1.getCollisionBox().y -= overlap.getHeight();
            }
        } else {
            if (bc1.getCollisionBox().getX() > bc2.getCollisionBox().getX()) {
                bc1.getCollisionBox().x += overlap.getWidth();
            } else {
                bc1.getCollisionBox().x -= overlap.getWidth();
            }
        }
    }

}
