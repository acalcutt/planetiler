package com.onthegomap.planetiler.basemap.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.basemap.BasemapProfile;
import com.onthegomap.planetiler.basemap.Layer;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.Translations;

public class atv implements BasemapProfile.OsmAllProcessor, Layer {

  public static final String LAYER_NAME = "atv";
  public static final int BUFFER_SIZE = 4;

  public atv(Translations translations, PlanetilerConfig config, Stats stats) {
  }


  @Override
  public void processAllOsm(SourceFeature feature, FeatureCollector features) {
    if (feature.canBeLine() && (feature.hasTag("atv") || feature.hasTag("ohv"))) {
      features.line(LAYER_NAME)
        .setBufferPixels(BUFFER_SIZE)
        .setMinZoom(10)
        .setAttr("class", "line");
    }
  }

  @Override
  public String name() {
    return LAYER_NAME;
  }
}
