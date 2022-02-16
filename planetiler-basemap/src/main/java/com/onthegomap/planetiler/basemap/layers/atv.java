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
    if (feature.hasTag("motorcycle") || feature.hasTag("vehicle") || feature.hasTag("4wd_only") || feature.hasTag("atv") || feature.hasTag("snowmobile") || feature.hasTag("ohv") || feature.hasTag("maxwidth")) {
      
      
      if (feature.canBeLine()) {
        features.line(LAYER_NAME)
          .setBufferPixels(BUFFER_SIZE)
          .setMinZoom(3)
          .setAttr("class", "line")
          .setAttr("name", feature.getTag("name"))
          .setAttr("motorcycle", feature.getTag("motorcycle"))
          .setAttr("vehicle", feature.getTag("vehicle"))
          .setAttr("4wd_only", feature.getTag("4wd_only"))
          .setAttr("atv", feature.getTag("atv"))
          .setAttr("snowmobile", feature.getTag("snowmobile"))
          .setAttr("toll", feature.getTag("toll"))
          .setAttr("ice_road", feature.getTag("ice_road"))
          .setAttr("operator", feature.getTag("operator"))
          .setAttr("website", feature.getTag("website"))
          .setAttr("grade", feature.getTag("grade"))
          .setAttr("maxwidth", feature.getTag("maxwidth"))
          .setAttr("maxspeed", feature.getTag("maxspeed"))
          .setAttr("ohv", feature.getTag("ohv"));
      } else if (feature.canBePolygon()) {
        features.polygon(LAYER_NAME)
          .setBufferPixels(BUFFER_SIZE)
          .setMinZoom(3)
          .setAttr("class", "polygon")
          .setAttr("name", feature.getTag("name"))
          .setAttr("motorcycle", feature.getTag("motorcycle"))
          .setAttr("vehicle", feature.getTag("vehicle"))
          .setAttr("4wd_only", feature.getTag("4wd_only"))
          .setAttr("atv", feature.getTag("atv"))
          .setAttr("snowmobile", feature.getTag("snowmobile"))
          .setAttr("ohv", feature.getTag("ohv"));
      } else if (feature.isPoint()) {
        features.point(LAYER_NAME)
          .setBufferPixels(BUFFER_SIZE)
          .setMinZoom(3)
          .setAttr("class", "point")
          .setAttr("name", feature.getTag("name"))
          .setAttr("motorcycle", feature.getTag("motorcycle"))
          .setAttr("vehicle", feature.getTag("vehicle"))
          .setAttr("4wd_only", feature.getTag("4wd_only"))
          .setAttr("atv", feature.getTag("atv"))
          .setAttr("snowmobile", feature.getTag("snowmobile"))
          .setAttr("amenity", feature.getTag("amenity"))
          .setAttr("tourism", feature.getTag("amenity"))
          .setAttr("ohv", feature.getTag("ohv"));
      }
    }
  }

  @Override
  public String name() {
    return LAYER_NAME;
  }
}
