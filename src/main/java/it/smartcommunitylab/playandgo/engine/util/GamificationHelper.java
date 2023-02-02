/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.engine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Shape;

/**
 * @author raman
 *
 */
@Component
public class GamificationHelper {

	private final static int EARTH_RADIUS = 6371; // Earth radius in km.

	public static void removeOutliers(List<Geolocation> points) {
		Set<Geolocation> toRemove = Sets.newHashSet();

		double averageSpeed = 0;

		double distance = 0;
		for (int i = 1; i < points.size(); i++) {
			double d = harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude());
			long t = points.get(i).getRecorded_at().getTime() - points.get(i - 1).getRecorded_at().getTime();
			if (t > 0) {
				distance += d;
			}
		}
		double time = points.get(points.size() - 1).getRecorded_at().getTime() - points.get(0).getRecorded_at().getTime();
		averageSpeed = 3600000 * distance / (double) time;

		for (int i = 1; i < points.size() - 1; i++) {
			double d1 = harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude());
			long t1 = points.get(i).getRecorded_at().getTime() - points.get(i - 1).getRecorded_at().getTime();
			double s1 = 0;
			if (t1 > 0) {
				s1 = 3600000 * d1 / (double) t1;
			}
			double d2 = harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude());
			long t2 = points.get(i + 1).getRecorded_at().getTime() - points.get(i).getRecorded_at().getTime();
			double s2 = 0;
			if (t2 > 0) {
				s2 = 3600000 * d2 / (double) t2;
			}

			Integer index = null;

			double d3 = harvesineDistance(points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude(), points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude());

			double a = Math.acos((d1 * d1 + d2 * d2 - d3 * d3) / (2 * d1 * d2));
			
			if (a < 0.017453292519943 * 3) {
				index = i;
			} else if (a < 0.017453292519943 * 30 && s1 > 4 * averageSpeed && s2 > 4 * averageSpeed && i != 1 && i != points.size() - 2) {
				index = i;
			} else if (i == 1 && s1 > 4 * averageSpeed) {
				index = 0;
			} else if (i == points.size() - 2 && s2 > 4 * averageSpeed) {
				index = points.size() - 1;
			}

			if (index != null) {
				toRemove.add(points.get(index));
			}
		}

		points.removeAll(toRemove);
	}

	public static List<Geolocation> optimize(List<Geolocation> geolocations) {
		List<Geolocation> points = new ArrayList<Geolocation>(geolocations);
		Collections.sort(points, new Comparator<Geolocation>() {

			@Override
			public int compare(Geolocation o1, Geolocation o2) {
				return (int) (o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime());
			}

		});		
		
		removeOutliers(points);
		return transform(points);
	}
	
	protected static boolean isMaximumTooFast(double speed, String ttype) {
		if ("walk".equals(ttype)) {
			if (speed > 20) {
				return true;
			}
		}
		if ("bike".equals(ttype)) {
			if (speed > 65) {
				return true;
			}
		}
		return false;
	}

	public static List<Geolocation> transform(List<Geolocation> points) {
		List<Geolocation> result = Lists.newArrayList();
		for (int i = 1; i < points.size(); i++) {
			transformPair(points.get(i - 1), points.get(i), result);
		}

		Collections.sort(result);

		return result;
	}

	private static void transformPair(Geolocation p1, Geolocation p2, List<Geolocation> result) {
		double distance = harvesineDistance(p1, p2);
		if (distance == 0) {
			return;
		}
		double[] lats = computeLats(p1, p2, distance);
		double[] lngs = computeLngs(p1, p2, distance);

//		Date[] recordedAt = computeRecordedAt(p1, p2);
		Geolocation p1n = new Geolocation(lats[0], lngs[0], p1.getRecorded_at());
		p1n.setCertificate(p1.getCertificate());
		Geolocation p2n = new Geolocation(lats[1], lngs[1], p2.getRecorded_at());
		p2n.setCertificate(p2.getCertificate());
		long acc = (p1.getAccuracy() + p2.getAccuracy()) / 2;
		p1n.setAccuracy(acc);
		p2n.setAccuracy(acc);
		result.add(p1n);
		result.add(p2n);

	}

	public static double[] compute(double v1, double a1, double v2, double a2, double distance) {
		
		// convert: meters to degree approx, considering 111km ~ 1
		double a1m = a1 / 1000.0 / 111.0, a2m = a2 / 1000.0 / 111.0;
		// projection
		a1m = a1m * (v2 - v1) / distance;
		a2m = a2m * (v2 - v1) / distance;
		if ((a1 + a2) / 1000.0 > distance) {
			// mid point
			double v = (v1 + v2 + a1m -  a2m) / 2;
			return new double[] { v, v };
		}
		return new double[] { v1 + a1m, v2 - a2m };
	}

	private static double[] computeLats(Geolocation p1, Geolocation p2, double distance) {
		if (p1.getLatitude() > p2.getLatitude()) {
			double[] res = computeLats(p2, p1, distance);
			return new double[] { res[1], res[0] };
		}
		return compute(p1.getLatitude(), (double) p1.getAccuracy(), p2.getLatitude(), (double) p2.getAccuracy(), distance);
	}

	private static double[] computeLngs(Geolocation p1, Geolocation p2, double distance) {
		if (p1.getLatitude() > p2.getLatitude()) {
			double[] res = computeLngs(p2, p1, distance);
			return new double[] { res[1], res[0] };
		}
		return compute(p1.getLongitude(), (double) p1.getAccuracy(), p2.getLongitude(), (double) p2.getAccuracy(), distance);
	}

	protected static Date[] computeRecordedAt(Geolocation p1, Geolocation p2) {
		Date[] res = new Date[2];
		if (p1.getRecorded_at().compareTo(p2.getRecorded_at()) < 0) {
			res[0] = p1.getRecorded_at();
			res[1] = p2.getRecorded_at();
		} else {
			res[0] = p2.getRecorded_at();
			res[1] = p1.getRecorded_at();
		}
		return res;
	}

	public static double harvesineDistance(Geolocation p1, Geolocation p2) {
		return harvesineDistance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
	}

	public static double harvesineDistance(double lat1, double lon1, double lat2, double lon2) {
		lat1 = Math.toRadians(lat1);
		lon1 = Math.toRadians(lon1);
		lat2 = Math.toRadians(lat2);
		lon2 = Math.toRadians(lon2);

		double dlon = lon2 - lon1;
		double dlat = lat2 - lat1;

		double a = Math.pow((Math.sin(dlat / 2)), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS * c;
	}

	public static List<List<Geolocation>> splitList(List<Geolocation> list) {
		List<List<Geolocation>> result = Lists.newArrayList();
		int half = list.size() / 2;
		List<Geolocation> l1 = list.subList(0, half);
		List<Geolocation> l2 = list.subList(half, list.size());
		result.add(l1);
		result.add(l2);
		return result;
	}

	public static String encodePoly(List<Geolocation> path) {

		StringBuffer encodedPoints = new StringBuffer();

		int plat = 0;
		int plng = 0;

		int listSize = path.size();

		Geolocation location;

		for (int i = 0; i < listSize; i++) {
			location = (Geolocation) path.get(i);

			int late5 = floor1e5(location.getLatitude());
			int lnge5 = floor1e5(location.getLongitude());

			int dlat = late5 - plat;
			int dlng = lnge5 - plng;

			plat = late5;
			plng = lnge5;

			encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng));
		}

		return encodedPoints.toString();

	}

	private static int floor1e5(double coordinate) {
		return (int) Math.floor(coordinate * 1e5);
	}

	private static String encodeSignedNumber(int num) {
		int sgn_num = num << 1;
		if (num < 0) {
			sgn_num = ~(sgn_num);
		}
		return (encodeNumber(sgn_num));
	}

	private static String encodeNumber(int num) {
		StringBuffer encodeString = new StringBuffer();

		while (num >= 0x20) {
			encodeString.append((char) ((0x20 | (num & 0x1f)) + 63));
			num >>= 5;
		}

		encodeString.append((char) (num + 63));

		return encodeString.toString();

	}

	public static String convertFreetrackingType(String tt) {
		if ("bus".equals(tt) || "train".equals(tt) || "boat".equals(tt)) {
			return "transit";
		}
		return tt;
	}	
	
	public static boolean inAreas(List<Shape> shapes, Geolocation point) {
		if (shapes != null) {
			for (Shape shape : shapes) {
				if (shape.inside(point.getLatitude(), point.getLongitude())) {
					return true;
				}
			}
		}
		return false;
	}

//	
//	public static void main(String[] args) {
//		String encoded = "yivpGsdpfA~AXxBr@lB`@`B\\pAXdBj@rA^tAThA`@bAVnBp@hAl@j@x@j@l@pAvB`@pAt@~BV~@ZfCb@zBVlBf@tCZbCXrBRvAThB\\bBZfCZxB`@|BTjBb@~B^zC^hC??b@|B\\zBZrB^jCb@xC`@vB\\dB\\pCZvBd@`DVpBr@hD\\nC^|B^vBd@vCn@jE\\lCZ|Bb@lC\\zBXxB`@`C\\lBVhBZrB^vBZrBXvB^tBXnB\\lBZvBf@|Bj@dC^dB`@jBh@dCj@nCl@dC`@xA`@pBh@hBl@bCd@xBh@`Cd@lB`@jBd@|A^nBb@nBl@dCf@vBd@tBh@lBn@hC`@hBh@hCj@rBj@xBd@tBb@bBf@zBl@jCl@dC^dBj@bCf@xBf@pBv@fDl@hCl@dCp@jCh@zCf@`D?nCQtCO~DEpDQlCQtCGxCIzBE|CBrDNlDPtCXnC^tC`@zCj@~C^lCNjC^~DJxCHrC@xC???~BCjCFvBDhECnB?vCAnCBxC@pBJxC??HzBNjBNnBN~BRbCHlCBlCC~DMnFSdDMlCMfDI|BSbD[xBm@nAs@xA{@zA{@xAuAzBuAxB}AjB{AjB}AtBuAnBqAhAgBhAoBlB_BlBeAjBe@rB[bBc@pCg@`Cy@`Ds@`Cy@fCoAtBu@~A}A|DaAvCsAjD{@|B{@~Cc@~By@lBu@~Aq@rBs@`B{@lCsA~Cs@nB{@zB}@~Bm@`C{@nCo@rBg@pBaApEq@rCk@~Bu@xCoA|Ck@jCs@|Bm@bC}@jCq@tBk@hBm@|Bm@jB}@xCaAvC??q@rBq@rBcAbC}@`CcAlCkApC{@jBeAfCaAfBmAtAsA`AgCx@yAn@oBv@eCdAyBv@sBp@_Bl@aCdAwBx@eCx@cBz@s@xAm@rBO~A]pBKrAQxA??QbBWjCObCMjCKtBK`CMhCUnESjBYfC_@jBWlBg@hCc@zCa@fC_@vB[rCe@rCe@bDm@vCa@jDi@rCc@tCe@dDg@zDe@jC]pBc@xCe@vCm@vDc@nC??a@lCe@|Be@xC_@`C_@zCe@xBc@zC{@tFg@~Ck@rC[hCc@bCYpB]hB[zB[bCi@dDe@nB[dC_@vBc@lC]zB]tBUfAWrBe@rDg@`CSnB_@pBc@fCe@vCi@|Cc@lCY|Bk@|Ce@~CYvBa@`Co@dC";
//		int index = 0, len = encoded.length();
//		int lat = 0, lng = 0;
//		int pos = 0;
//		List<Geolocation> list = new ArrayList<>();
//		while (index < len) {
//			int b, shift = 0, result = 0;
//			do {
//				b = encoded.charAt(index++) - 63;
//				result |= (b & 0x1f) << shift;
//				shift += 5;
//			} while (b >= 0x20);
//			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//			lat += dlat;
//			shift = 0;
//			result = 0;
//			do {
//				b = encoded.charAt(index++) - 63;
//				result |= (b & 0x1f) << shift;
//				shift += 5;
//			} while (b >= 0x20);
//			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//			lng += dlng;
//
//			Geolocation onLeg = new Geolocation();
//			onLeg.setLatitude((((double) lat / 1E5)));
//			onLeg.setLongitude((((double) lng / 1E5)));
//			list.add(onLeg);
//		}
//		
//		Collections.reverse(list);
//		pos = 0;
//		for (Geolocation onLeg : list) {
//			System.err.println("BR,"+onLeg.getLatitude()+","+onLeg.getLongitude()+","+pos++);
//
//		}
//	}
}
