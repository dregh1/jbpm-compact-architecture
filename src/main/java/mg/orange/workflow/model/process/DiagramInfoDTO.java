package mg.orange.workflow.model.process;

import java.util.Map;

/**
 * DTO représentant les informations du diagramme BPMN (coordonnées et layout)
 */
public class DiagramInfoDTO {

    private Map<String, ShapeInfo> shapes;
    private Map<String, EdgeInfo> edges;

    public DiagramInfoDTO() {
    }

    // Getters et Setters
    public Map<String, ShapeInfo> getShapes() {
        return shapes;
    }

    public void setShapes(Map<String, ShapeInfo> shapes) {
        this.shapes = shapes;
    }

    public Map<String, EdgeInfo> getEdges() {
        return edges;
    }

    public void setEdges(Map<String, EdgeInfo> edges) {
        this.edges = edges;
    }

    /**
     * Informations de positionnement d'une forme (nœud)
     */
    public static class ShapeInfo {
        private String elementId;
        private double x;
        private double y;
        private double width;
        private double height;

        public ShapeInfo() {
        }

        public ShapeInfo(String elementId, double x, double y, double width, double height) {
            this.elementId = elementId;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        // Getters et Setters
        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }
    }

    /**
     * Informations sur une connexion (edge) entre nœuds
     */
    public static class EdgeInfo {
        private String elementId;
        private String sourceId;
        private String targetId;
        private java.util.List<Waypoint> waypoints;

        public EdgeInfo() {
        }

        // Getters et Setters
        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public java.util.List<Waypoint> getWaypoints() {
            return waypoints;
        }

        public void setWaypoints(java.util.List<Waypoint> waypoints) {
            this.waypoints = waypoints;
        }

        /**
         * Point de passage d'une connexion
         */
        public static class Waypoint {
            private double x;
            private double y;

            public Waypoint() {
            }

            public Waypoint(double x, double y) {
                this.x = x;
                this.y = y;
            }

            public double getX() {
                return x;
            }

            public void setX(double x) {
                this.x = x;
            }

            public double getY() {
                return y;
            }

            public void setY(double y) {
                this.y = y;
            }
        }
    }
}
