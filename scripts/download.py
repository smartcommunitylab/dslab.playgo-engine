from pyrosm import get_data
from pyrosm import OSM
import geopandas as gd
import json 
import pandas as pd

fp = get_data("nord_ovest")
osm = OSM(fp)
# osm = OSM('./nord-ovest-latest.osm.pbf')
# osm = OSM(get_data("warwickshire"))

# Test reading all transit related data (bus, trains, trams, metro etc.)
# Exclude nodes (not keeping stops, etc.)
routes = ["train"]
transit = osm.get_data_by_custom_criteria(custom_filter={ 'route': routes},
                                        # Keep data matching the criteria above
                                        filter_type="keep",
                                        # Do not keep nodes (point data)    
                                        keep_nodes=False, 
                                        keep_ways=True, 
                                        keep_relations=True)
transit.to_file('./transit.geojson', driver='GeoJSON')

gdf = gd.read_file('./transit.geojson')
treni = gdf#[(gdf['network'] == 'STIBM') | (gdf['network'] ==  'Servizio ferroviario regionale della Lombardia')  | (gdf['network'] == 'Trenitalia')]
treni1 = treni[['from', 'to', 'network', 'tags', 'geometry']]

def transn(e):
    tags = json.loads(e if e is not None else '{}')
    return (tags['name'] if 'name' in tags else '')

def transr(e):
    tags = json.loads(e if e is not None else '{}')
    return (tags['ref'] if 'ref' in tags else '')

treni1['ref'] = gdf['tags'].apply(transr)
treni1['name'] = gdf['tags'].apply(transn)

treni1 = treni1[
    (treni1['ref'] == 'R11') |
    (treni1['ref'] == 'R14') |
    (treni1['ref'] == 'R16') |
    (treni1['ref'] == 'R7') |
    (treni1['ref'] == 'R28') |
    (treni1['ref'] == 'RE1') |
    (treni1['ref'] == 'RE4') |
    (treni1['ref'] == 'RE8') |
    (treni1['ref'] == 'RE11') |
    (treni1['ref'] == 'R22') |
    (treni1['ref'] == 'RE80') |
    (treni1['ref'] == 'RE81' ) |
    (treni1['ref'] == 'S1') |
    (treni1['ref'] == 'S2') |
    (treni1['ref'] == 'S5') |
    (treni1['ref'] == 'S7') |
    (treni1['ref'] == 'S8') |
    (treni1['ref'] == 'S9') |
    (treni1['ref'] == 'S11') |
    (treni1['ref'] == 'S12') |
    (treni1['ref'] == 'S13') |
    (treni1['ref'] == 'MXP2')
]

treni2 = treni1.explode()
recs = treni2.to_dict('records')

res = []
last = None
pos = 0
for r in recs:
    name = r['name'] if r['name'] != '' else r['ref']
    if name != last:
        last = name
        pos = 0
    poly = [{'lat': coords[1],'lon': coords[0]} for coords in r['geometry'].coords]
    for i in range(len(poly)):
        pos = pos + 1
        sr = {}
        c = poly[i]
        sr = {'shape_id': name, 'shape_pt_lat': c['lat'], 'shape_pt_lon': c['lon'], 'shape_pt_sequence': pos}
        res.append(sr)
df = pd.DataFrame.from_dict(res) 
df.to_csv('./treni.csv', index=False)