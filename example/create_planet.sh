#!/bin/bash

PBF=planet-latest.osm.pbf
echo "====> : Start importing Planet OpenStreetMap data: data/$PBF -> imposm3[./config/mapping.yaml] -> PostgreSQL"

EXPORT_DIR=$(pwd)/data
mkdir -p $EXPORT_DIR

#Download the planet pdb
if [ ! -f $EXPORT_DIR/$PBF ]; then
	download-osm planet -o $EXPORT_DIR/$PBF
	#wget $PBF_BASE$PBF -P $EXPORT_DIR
fi

#java -jar planetiler.jar --download --osm-path=$EXPORT_DIR/$PBF
#java -Xmx90g -Xms90g \
#	-XX:OnOutOfMemoryError="kill -9 %p" \
#	-jar planetiler-dist-0.3-SNAPSHOT-with-deps.jar \
#	--download --osm-path=$EXPORT_DIR/$PBF \
#	--download-threads=10 --download-chunk-size-mb=1000 \
#	--fetch-wikidata \
#	--mbtiles=output.mbtiles \
#	--nodemap-type=sparsearray --nodemap-storage=ram --optimize_db=true 

#example for 24 cpu / 115GB Memory

java -Xmx100g -Xms100g \
	-XX:OnOutOfMemoryError="kill -9 %p" \
	-jar planetiler-dist-0.3-SNAPSHOT-with-deps.jar \
	--download --osm-path=$EXPORT_DIR/$PBF \
	--download-threads=10 --download-chunk-size-mb=1000 \
	--fetch-wikidata --extra_layers=power,atv \
	--mbtiles=output.mbtiles \
	--nodemap-type=sparsearray --nodemap-storage=mmap --optimize_db=true

