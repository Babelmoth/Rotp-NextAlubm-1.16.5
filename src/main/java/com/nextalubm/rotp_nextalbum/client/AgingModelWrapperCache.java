package com.nextalubm.rotp_nextalbum.client;

import net.minecraft.client.renderer.model.IBakedModel;

public final class AgingModelWrapperCache {

    private static final ThreadLocal<IBakedModel> lastOriginal = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<AgingBakedModelWrapper> lastWrapper = ThreadLocal.withInitial(() -> null);

    private AgingModelWrapperCache() {
    }

    public static IBakedModel wrap(IBakedModel model) {
        if (model == null) {
            return null;
        }
        if (model instanceof AgingBakedModelWrapper) {
            return model;
        }
        IBakedModel cached = lastOriginal.get();
        AgingBakedModelWrapper wrapper = lastWrapper.get();
        if (model == cached && wrapper != null) {
            return wrapper;
        }
        AgingBakedModelWrapper newWrapper = new AgingBakedModelWrapper(model);
        lastOriginal.set(model);
        lastWrapper.set(newWrapper);
        return newWrapper;
    }
}
