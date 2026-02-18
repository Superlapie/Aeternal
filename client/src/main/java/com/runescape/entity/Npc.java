package com.runescape.entity;

import com.runescape.Client;
import com.runescape.cache.anim.Animation;
import com.runescape.cache.anim.Frame;
import com.runescape.cache.anim.Graphic;
import com.runescape.cache.def.NpcDefinition;
import com.runescape.entity.model.Model;

public final class Npc extends Mob {

    public NpcDefinition desc;
    public int headIcon = -1;
    public int ownerIndex = -1;

    public boolean showActions() {
        if (ownerIndex == -1) {
            return true;
        }
        return (Client.instance.localPlayerIndex == ownerIndex);
    }

    public int getHeadIcon() {
        if (headIcon == -1) {
            if (desc != null) {
                return desc.headIcon;
            }
        }
        return headIcon;
    }

    private Model getAnimatedModel() {
        int emoteFrame = -1;
        int movementFrame = -1;
        int[] interleave = null;

        if (super.emoteAnimation >= 0 && super.animationDelay == 0
                && super.emoteAnimation < Animation.animations.length) {
            Animation emoteAnim = Animation.animations[super.emoteAnimation];
            if (emoteAnim != null
                    && !emoteAnim.isSkeletalSequence()
                    && emoteAnim.primaryFrames != null
                    && super.displayedEmoteFrames >= 0
                    && super.displayedEmoteFrames < emoteAnim.primaryFrames.length) {
                emoteFrame = emoteAnim.primaryFrames[super.displayedEmoteFrames];
                interleave = emoteAnim.interleaveOrder;
            }
        }

        if (super.movementAnimation >= 0
                && super.movementAnimation < Animation.animations.length
                && (super.movementAnimation != super.idleAnimation || emoteFrame == -1)) {
            Animation movementAnim = Animation.animations[super.movementAnimation];
            if (movementAnim != null
                    && !movementAnim.isSkeletalSequence()
                    && movementAnim.primaryFrames != null
                    && super.displayedMovementFrames >= 0
                    && super.displayedMovementFrames < movementAnim.primaryFrames.length) {
                movementFrame = movementAnim.primaryFrames[super.displayedMovementFrames];
            }
        }

        // 2446 Yama body anims are skeletal/maya and not directly playable in this client.
        // Reject invalid/unsupported transform frames so Yama stays visible instead of warping.
        if (desc != null && (desc.id == 13243 || desc.id == 14176 || desc.id == 15555)) {
            if (movementFrame != -1 && Frame.method531(movementFrame) == null) {
                movementFrame = -1;
            }
            if (emoteFrame != -1 && Frame.method531(emoteFrame) == null) {
                emoteFrame = -1;
            }
        }

        return desc.getAnimatedModel(movementFrame, emoteFrame, interleave);
    }

    public Model getRotatedModel() {
        if (desc == null)
            return null;
        Model animatedModel = getAnimatedModel();
        if (animatedModel == null)
            return null;
        super.height = animatedModel.modelBaseY;
        if (super.graphic != -1 && super.currentAnimation != -1) {
            Graphic spotAnim = Graphic.cache[super.graphic];
            Model graphicModel = spotAnim.getModel();
            if (graphicModel != null) {
                int frame = spotAnim.animationSequence.primaryFrames[super.currentAnimation];
                Model model = new Model(true, Frame.noAnimationInProgress(frame),
                        false, graphicModel);
                model.translate(0, -super.graphicHeight, 0);
                model.skin();
                model.applyTransform(frame);
                model.faceGroups = null;
                model.vertexGroups = null;
                if (spotAnim.resizeXY != 128 || spotAnim.resizeZ != 128)
                    model.scale(spotAnim.resizeXY, spotAnim.resizeXY,
                            spotAnim.resizeZ);
                model.light(64 + spotAnim.modelBrightness,
                        850 + spotAnim.modelShadow, -30, -50, -30, true);
                Model[] models = {animatedModel, model};
                animatedModel = new Model(models);
            }
        }
        if (desc.size == 1)
            animatedModel.fits_on_single_square = true;
        return animatedModel;
    }

    public boolean isVisible() {
        return desc != null;
    }
}
