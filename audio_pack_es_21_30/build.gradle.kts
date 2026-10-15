plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_es_21_30")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
