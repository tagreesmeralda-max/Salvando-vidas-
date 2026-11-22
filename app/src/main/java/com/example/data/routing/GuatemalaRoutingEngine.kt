package com.example.data.routing

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GuatemalaNode(
    val id: String,
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val emergencyHub: String,
    val emergencyPhone: String
)

data class HighwayEdge(
    val fromId: String,
    val toId: String,
    val corridor: String,
    val distanceKm: Double,
    val carSpeedKmh: Double,
    val motoSpeedKmh: Double,
    val mountainFactor: Double = 1.0
)

data class RouteSegment(
    val fromName: String,
    val toName: String,
    val corridor: String,
    val distanceKm: Double,
    val estimatedMinutes: Int
)

data class OptimalRouteResult(
    val origin: GuatemalaNode,
    val destination: GuatemalaNode,
    val vehicleType: String,
    val totalDistanceKm: Double,
    val totalMinutes: Int,
    val pathNodes: List<GuatemalaNode>,
    val segments: List<RouteSegment>,
    val advisory: String
)

/**
 * National Guatemala 22-department road network and emergency hub router.
 * Operates both online and 100% offline with Dijkstra optimization tailored for CARRO vs MOTO.
 */
object GuatemalaRoutingEngine {
    val departments: List<GuatemalaNode> = listOf(
        GuatemalaNode("GUA", "Guatemala", "Metropolitana", 14.6349, -90.5069, "Hospital Roosevelt / Bomberos Voluntarios", "122"),
        GuatemalaNode("SAC", "Sacatepéquez", "Central", 14.5586, -90.7295, "Hospital Nacional Pedro de Bethancourt", "122"),
        GuatemalaNode("CHM", "Chimaltenango", "Central", 14.6611, -90.8194, "Hospital Nacional de Chimaltenango", "122"),
        GuatemalaNode("ESC", "Escuintla", "Sur", 14.3050, -90.7850, "Hospital Nacional de Escuintla / IGSS", "123"),
        GuatemalaNode("PRO", "El Progreso", "Nororiente", 14.8500, -90.0667, "Hospital Nacional de Guastatoya", "122"),
        GuatemalaNode("SRO", "Santa Rosa", "Suroriente", 14.2797, -90.2989, "Hospital Regional de Cuilapa", "122"),
        GuatemalaNode("SOL", "Sololá", "Altiplano", 14.7722, -91.1833, "Hospital Nacional de Sololá", "122"),
        GuatemalaNode("TOT", "Totonicapán", "Occidente", 14.9117, -91.3611, "Hospital Departamental Totonicapán", "122"),
        GuatemalaNode("QUE", "Quetzaltenango", "Occidente", 14.8333, -91.5167, "Hospital Regional de Occidente (HRO)", "122"),
        GuatemalaNode("SUC", "Suchitepéquez", "Suroccidente", 14.5333, -91.5033, "Hospital Nacional de Mazatenango", "122"),
        GuatemalaNode("RET", "Retalhuleu", "Suroccidente", 14.5361, -91.6778, "Hospital Nacional de Retalhuleu", "122"),
        GuatemalaNode("SMA", "San Marcos", "Occidente", 14.9639, -91.7944, "Hospital Nacional de San Marcos", "122"),
        GuatemalaNode("HUE", "Huehuetenango", "Noroccidente", 15.3197, -91.4708, "Hospital Regional de Huehuetenango", "122"),
        GuatemalaNode("QUI", "Quiché", "Noroccidente", 15.0306, -91.1489, "Hospital Regional Santa Elena", "122"),
        GuatemalaNode("BVE", "Baja Verapaz", "Verapaces", 15.1000, -90.3167, "Hospital Nacional de Salamá", "122"),
        GuatemalaNode("AVE", "Alta Verapaz", "Verapaces", 15.4708, -90.3708, "Hospital Regional Hellen Lossi Cobán", "122"),
        GuatemalaNode("PET", "Petén", "Norte", 16.9297, -89.8925, "Hospital Regional de San Benito", "122"),
        GuatemalaNode("IZA", "Izabal", "Caribe", 15.7278, -88.5944, "Hospital Nacional de Puerto Barrios", "122"),
        GuatemalaNode("ZAC", "Zacapa", "Oriente", 14.9722, -89.5306, "Hospital Regional de Zacapa", "122"),
        GuatemalaNode("CHQ", "Chiquimula", "Oriente", 14.8000, -89.5458, "Hospital Modular Carlos Manuel Arana", "122"),
        GuatemalaNode("JAL", "Jalapa", "Oriente", 14.6333, -89.9833, "Hospital Nacional Nicolasa Cruz", "122"),
        GuatemalaNode("JUT", "Jutiapa", "Suroriente", 14.2917, -89.8958, "Hospital Nacional Ernestina García", "122")
    )

    private val nodeMap = departments.associateBy { it.id }

    private val edges: List<HighwayEdge> = buildList {
        fun bi(from: String, to: String, corridor: String, km: Double, car: Double, moto: Double, mountain: Double = 1.0) {
            add(HighwayEdge(from, to, corridor, km, car, moto, mountain))
            add(HighwayEdge(to, from, corridor, km, car, moto, mountain))
        }
        // Central & Metropolitan Ring
        bi("GUA", "SAC", "RN-10 / Carretera a Antigua", 38.0, 42.0, 56.0, 1.15)
        bi("GUA", "CHM", "CA-1 Occidente (Interamericana)", 54.0, 52.0, 64.0, 1.10)
        bi("SAC", "CHM", "RN-14", 24.0, 44.0, 54.0, 1.10)
        bi("SAC", "ESC", "RN-14 Volcán de Fuego", 48.0, 50.0, 60.0, 1.20)
        bi("GUA", "ESC", "CA-9 Sur / Autopista Palín", 58.0, 74.0, 78.0, 1.0)
        bi("GUA", "PRO", "CA-9 Norte (Ruta al Atlántico)", 73.0, 65.0, 74.0, 1.05)
        bi("GUA", "SRO", "CA-1 Oriente", 64.0, 58.0, 68.0, 1.05)
        bi("GUA", "JAL", "RN-19 Mataquescuintla", 96.0, 46.0, 58.0, 1.25)

        // Occidente & Altiplano
        bi("CHM", "SOL", "CA-1 Occidente / RN-1", 68.0, 48.0, 60.0, 1.25)
        bi("CHM", "QUI", "RN-15 Los Encuentros", 86.0, 46.0, 56.0, 1.25)
        bi("SOL", "TOT", "CA-1 Occidente Cumbre Alaska", 42.0, 45.0, 56.0, 1.30)
        bi("SOL", "SUC", "RN-11 Patulul", 58.0, 44.0, 54.0, 1.25)
        bi("TOT", "QUE", "RN-1 Cuatro Caminos", 26.0, 52.0, 62.0, 1.10)
        bi("TOT", "QUI", "RN-15", 46.0, 44.0, 54.0, 1.20)
        bi("TOT", "HUE", "CA-1 Occidente", 78.0, 54.0, 64.0, 1.20)
        bi("QUE", "SMA", "RN-1 Palestina", 52.0, 48.0, 58.0, 1.25)
        bi("QUE", "RET", "Cito-Zarco RN-9S", 56.0, 54.0, 65.0, 1.20)
        bi("QUE", "SUC", "CA-2 / Cito-Zarco", 64.0, 56.0, 66.0, 1.15)
        bi("QUE", "HUE", "CA-1 / Cuatro Caminos", 88.0, 54.0, 64.0, 1.20)

        // Costa Sur CA-2
        bi("ESC", "SRO", "CA-2 Oriente Chiquimulilla", 76.0, 66.0, 72.0, 1.0)
        bi("ESC", "SUC", "CA-2 Occidente Cocales", 98.0, 72.0, 76.0, 1.0)
        bi("SUC", "RET", "CA-2 Occidente", 28.0, 68.0, 74.0, 1.0)
        bi("RET", "SMA", "CA-2 / Coatepeque - Pajapita", 74.0, 62.0, 70.0, 1.05)

        // Noroccidente & Verapaces & Norte
        bi("HUE", "QUI", "RN-7W Sacapulas", 82.0, 38.0, 48.0, 1.35)
        bi("QUI", "AVE", "FTN / Cobán - Uspantán", 118.0, 40.0, 50.0, 1.35)
        bi("PRO", "BVE", "CA-14 El Rancho - Salamá", 74.0, 50.0, 60.0, 1.25)
        bi("BVE", "AVE", "CA-14 Las Verapaces", 62.0, 48.0, 58.0, 1.25)
        bi("AVE", "PET", "CA-13 / FTN Chisec - Sayaxché", 248.0, 60.0, 66.0, 1.05)
        bi("IZA", "PET", "CA-13 Río Dulce - Poptún - Flores", 232.0, 64.0, 70.0, 1.0)

        // Nororiente, Oriente & Suroriente
        bi("PRO", "ZAC", "CA-9 Norte Río Hondo", 68.0, 72.0, 76.0, 1.0)
        bi("PRO", "JAL", "RN-19 Sanarate", 52.0, 48.0, 58.0, 1.20)
        bi("ZAC", "IZA", "CA-9 Norte Morales - Puerto Barrios", 148.0, 70.0, 74.0, 1.0)
        bi("ZAC", "CHQ", "CA-10", 24.0, 64.0, 72.0, 1.05)
        bi("CHQ", "JAL", "RN-18 Ipala", 68.0, 48.0, 58.0, 1.20)
        bi("CHQ", "JUT", "CA-10 / Ipala", 76.0, 52.0, 60.0, 1.15)
        bi("JAL", "JUT", "RN-19 Monjas", 64.0, 52.0, 62.0, 1.15)
        bi("SRO", "JUT", "CA-1 Oriente", 54.0, 60.0, 68.0, 1.10)
    }

    fun departmentByName(name: String): GuatemalaNode {
        return departments.firstOrNull {
            it.name.equals(name.trim(), ignoreCase = true) ||
                it.id.equals(name.trim(), ignoreCase = true)
        } ?: departments.first()
    }

    fun findNearestDepartment(lat: Double, lng: Double): GuatemalaNode {
        return departments.minByOrNull { node ->
            haversineKm(lat, lng, node.latitude, node.longitude)
        } ?: departments.first()
    }

    fun calculateOptimalRoute(
        originName: String,
        destinationName: String,
        vehicleType: String
    ): OptimalRouteResult {
        val origin = departmentByName(originName)
        val destination = departmentByName(destinationName)
        val isMoto = vehicleType.equals("MOTO", ignoreCase = true)

        if (origin.id == destination.id) {
            val localSegment = RouteSegment(
                fromName = origin.name,
                toName = origin.emergencyHub,
                corridor = "Corredor Urbano Local (${origin.name})",
                distanceKm = 4.2,
                estimatedMinutes = if (isMoto) 7 else 11
            )
            return OptimalRouteResult(
                origin = origin,
                destination = destination,
                vehicleType = if (isMoto) "MOTO" else "CARRO",
                totalDistanceKm = localSegment.distanceKm,
                totalMinutes = localSegment.estimatedMinutes,
                pathNodes = listOf(origin),
                segments = listOf(localSegment),
                advisory = if (isMoto) {
                    "Modo Moto: filtrado ágil en tráfico urbano hacia ${origin.emergencyHub}. Uso obligatorio de casco."
                } else {
                    "Modo Carro: ruta arterial principal hacia ${origin.emergencyHub} apta para ambulancia/grúa."
                }
            )
        }

        // Dijkstra minimizing travel time (minutes) for selected vehicle type
        val distTime = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val prevEdge = mutableMapOf<String, HighwayEdge>()
        val visited = mutableSetOf<String>()
        distTime[origin.id] = 0.0

        while (visited.size < departments.size) {
            val current = distTime.entries
                .filter { it.key !in visited }
                .minByOrNull { it.value }?.key ?: break
            if (current == destination.id) break
            visited.add(current)

            val currentCost = distTime.getValue(current)
            for (edge in edges.filter { it.fromId == current }) {
                val speed = if (isMoto) edge.motoSpeedKmh else edge.carSpeedKmh
                val travelHours = (edge.distanceKm / speed) * edge.mountainFactor
                val minutes = travelHours * 60.0
                val alt = currentCost + minutes
                if (alt < distTime.getValue(edge.toId)) {
                    distTime[edge.toId] = alt
                    prevEdge[edge.toId] = edge
                }
            }
        }

        val reconstructedEdges = mutableListOf<HighwayEdge>()
        var curr = destination.id
        while (curr != origin.id) {
            val edge = prevEdge[curr] ?: break
            reconstructedEdges.add(edge)
            curr = edge.fromId
        }
        reconstructedEdges.reverse()

        val nodes = buildList {
            add(origin)
            reconstructedEdges.forEach { edge ->
                nodeMap[edge.toId]?.let { add(it) }
            }
        }

        val segments = reconstructedEdges.map { edge ->
            val fromNode = nodeMap[edge.fromId] ?: origin
            val toNode = nodeMap[edge.toId] ?: destination
            val speed = if (isMoto) edge.motoSpeedKmh else edge.carSpeedKmh
            val mins = ((edge.distanceKm / speed) * edge.mountainFactor * 60.0).toInt().coerceAtLeast(1)
            RouteSegment(
                fromName = fromNode.name,
                toName = toNode.name,
                corridor = edge.corridor,
                distanceKm = edge.distanceKm,
                estimatedMinutes = mins
            )
        }

        val totalKm = segments.sumOf { it.distanceKm }
        val totalMin = segments.sumOf { it.estimatedMinutes }
        val advisory = if (isMoto) {
            "Ruta óptima Moto (${segments.size} tramos): prioriza maniobrabilidad en tráfico y pasos de montaña hacia ${destination.name}."
        } else {
            "Ruta óptima Carro (${segments.size} tramos): prioriza carreteras troncales CA/RN con cobertura de grúa y ambulancia hacia ${destination.name}."
        }

        return OptimalRouteResult(
            origin = origin,
            destination = destination,
            vehicleType = if (isMoto) "MOTO" else "CARRO",
            totalDistanceKm = totalKm,
            totalMinutes = totalMin,
            pathNodes = nodes,
            segments = segments,
            advisory = advisory
        )
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
