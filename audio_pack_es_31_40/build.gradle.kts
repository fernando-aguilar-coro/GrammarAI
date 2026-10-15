plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_es_31_40")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
