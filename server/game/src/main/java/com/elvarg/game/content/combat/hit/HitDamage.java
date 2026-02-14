package com.elvarg.game.content.combat.hit;

/**
 * A hit done by an entity onto a target.
 *
 * @author Gabriel Hannason
 */
public class HitDamage {

	private int damage;
	private HitMask hitmask;
    private HitMask startHitmask;
	private com.elvarg.game.entity.impl.Mobile attacker;

	public HitDamage(int damage, HitMask hitmask) {
		this(null, damage, hitmask);
	}

	public HitDamage(com.elvarg.game.entity.impl.Mobile attacker, int damage, HitMask hitmask) {
		this.attacker = attacker;
		this.damage = damage;
		this.hitmask = hitmask;
		this.startHitmask = hitmask;
		update();
	}

	public int getDamage() {
		return damage;
	}

	public void setDamage(int damage) {
		this.damage = damage;
		update();
	}

	public void incrementDamage(int damage) {
		this.damage += damage;
		update();
	}

	public void multiplyDamage(double mod) {
		this.damage *= mod;
		update();
	}

	public void update() {
	    if (damage > 0) {
            hitmask = startHitmask == HitMask.BLUE ? HitMask.RED : startHitmask;
        } else {
            damage = 0;
            hitmask = HitMask.BLUE;
        }
	}

	public HitMask getHitmask() {
		return hitmask;
	}

	public void setHitmask(HitMask hitmask) {
		this.hitmask = hitmask;
	}

	public com.elvarg.game.entity.impl.Mobile getAttacker() {
		return attacker;
	}
}
