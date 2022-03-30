package it.smartcommunitylab.playandgo.engine.influxdb;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class InfluxDbManager {
	private static transient final Logger logger = LoggerFactory.getLogger(InfluxDbManager.class);
	
//	InfluxDBClient influxDBClient;
	
//	@PostConstruct
//	public void init() {
//		char[] token = "zp71Blbz5lBlZRb75Cle5WbYQn1AosrqqYXCpC7Mxx_0fE_vMeGyjyvYtXi0ExgyqxqzxIwyF2SETgu_2az1fg==".toCharArray();
//		influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", token, "FBK", "playandgo");
//	}
	
	public void storeData(List<PlayerStatsTrackMeasurement> list) {
//		WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
//		writeApi.writeMeasurements("playandgo", "FBK", WritePrecision.NS, list);
	}
	
	public void queryData(String campaignId, String modeType, Date dateFrom, Date dateTo, Pageable pageRequest) {
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		String flux = "from(bucket:\"playandgo\")"
//				+ " |> range(start: " + sdf.format(dateFrom) + ", stop: " + sdf.format(dateTo) + ")"
//				+ " |> filter(fn: (r) => r[\"_measurement\"] == \"playerStatsTrack\")"
//				+ " |> filter(fn: (r) => r[\"modeType\"] == \"" + modeType + "\")"
//				+ " |> filter(fn: (r) => r[\"campaignId\"] == \"" + campaignId + "\")"
//				+ " |> group(columns: [\"playerId\"])"
//				+ " |> sum() |> group()"
//				+ " |> sort(columns: [\"_value\"], desc: true)"
//				+ " |> limit(n:" + pageRequest.getPageSize() + ", offset:" + (pageRequest.getPageNumber() * pageRequest.getPageSize()) + ")";
//		QueryApi queryApi = influxDBClient.getQueryApi();
//		List<FluxTable> tables = queryApi.query(flux);
//		for (FluxTable fluxTable : tables) {
//			List<FluxRecord> records = fluxTable.getRecords();
//            for (FluxRecord r : records) {
//                //logger.info(String.format("%s - %s - %s", r.getTime(), r.getValueByKey("playerId"), r.getValueByKey("_value")));
//            }			
//		}
//		
	}
}
