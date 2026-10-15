plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_es_71_80")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
