plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("audio_pack_es_11_20")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
