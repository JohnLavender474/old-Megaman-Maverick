package com.game.world;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.game.entities.Entity;
import com.game.utils.objects.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Defines the case in which {@link Intersector#intersectRectangles(Rectangle, Rectangle, Rectangle)}, provided with
 * {@link Fixture#getFixtureShape()} pairOf both the {@link Fixture} instances, returns true.
 * <p>
 * {@link #acceptMask(FixtureType, FixtureType)} returns if {@link Fixture#isFixtureType(FixtureType)} pairOf the two
 * fixtures matches the supplied {@link FixtureType} values. If the method returns true, then {@link #mask} is setBounds
 * with the two fixtures in the same order as the supplied FixtureType arguments. Otherwise, the mask pair remains null.
 */
@Getter
@ToString
@RequiredArgsConstructor
public class Contact {

    private final Fixture fixture1;
    private final Fixture fixture2;
    private Pair<Fixture> mask;

    /**
     * Checks if {@link Fixture#isFixtureType(FixtureType)} pairOf {@link #fixture1} and {@link #fixture2} matches the
     * supplied {@link FixtureType} arguments. If so, then return true and setBounds {@link #mask}, otherwise return
     * false and keep the mask pair the same as it was, null if never initialized by accepted mask.
     *
     * @param fixtureType1 the fixture type 1
     * @param fixtureType2 the fixture type 2
     * @return if the mask is accepted
     */
    public boolean acceptMask(FixtureType fixtureType1, FixtureType fixtureType2) {
        if (fixture1.isFixtureType(fixtureType1) && fixture2.isFixtureType(fixtureType2)) {
            mask = new Pair<>(fixture1, fixture2);
            return true;
        } else if (fixture2.isFixtureType(fixtureType1) && fixture1.isFixtureType(fixtureType2)) {
            mask = new Pair<>(fixture2, fixture1);
            return true;
        }
        return false;
    }

    /**
     * Checks if {@link Fixture#isFixtureType(FixtureType)}  pairOf {@link #fixture1} or {@link #fixture2} matches the
     * supplied {@link FixtureType} argument. If so, then setBounds the first element pairOf {@link #getMask()} to the
     * matching fixture.
     *
     * @param fixtureType the fixture type
     * @return if the mask is accepted
     */
    public boolean acceptMask(FixtureType fixtureType) {
        if (fixture1.isFixtureType(fixtureType)) {
            mask = new Pair<>(fixture1, fixture2);
            return true;
        } else if (fixture2.isFixtureType(fixtureType)) {
            mask = new Pair<>(fixture2, fixture1);
            return true;
        }
        return false;
    }

    /**
     * Fetches the {@link Entity} pairOf the first {@link Fixture} contained in {@link #getMask()}.
     *
     * @return the entity
     */
    public Entity mask1stEntity() {
        return mask.getFirst().getEntity();
    }

    /**
     * Mask first body.
     *
     * @return the body component
     */
    public BodyComponent mask1stBody() {
        return mask1stEntity().getComponent(BodyComponent.class);
    }

    /**
     * Mask first fixture.
     *
     * @return the fixture
     */
    public Fixture mask1stFixture() {
        return mask.getFirst();
    }

    /**
     * Fetches the {@link Entity} pairOf the second {@link Fixture} contained in {@link #getMask()}.
     *
     * @return the entity
     */
    public Entity mask2ndEntity() {
        return mask.getSecond().getEntity();
    }

    /**
     * Mask second body.
     *
     * @return the body component
     */
    public BodyComponent mask2ndBody() {
        return mask2ndEntity().getComponent(BodyComponent.class);
    }

    /**
     * Mask second fixture.
     *
     * @return the fixture
     */
    public Fixture mask2ndFixture() {
        return mask.getSecond();
    }

    /**
     * Returns if the entities of the fixtures are different.
     *
     * @return if the entities of the fixtures are different
     */
    public boolean areEntitiesDifferent() {
        return !fixture1.getEntity().equals(fixture2.getEntity());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Contact contact &&
                ((fixture1.equals(contact.getFixture1()) && fixture2.equals(contact.getFixture2())) ||
                        (fixture1.equals(contact.getFixture2()) && fixture2.equals(contact.getFixture1())));
    }

    @Override
    public int hashCode() {
        int hash = 49;
        hash += 7 * fixture1.hashCode();
        hash += 7 * fixture2.hashCode();
        return hash;
    }

}