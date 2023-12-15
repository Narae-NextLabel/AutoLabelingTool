var tools = {
    labelling : {


        "tool-rectangle" : {
            type: "rect",
            title  : "box",
            desp : "Create a Boundary boxrectangle",
            icon : "rectangle.svg",
            drawable : true,
            create : function(){
                var rect =  myCanvas.nested().rect().addClass('labelbox shape')/* .draw() */;
                rect.resize();
                rect.parent().draggable();
                return rect;
            },
            validate: function(el){
                return Number.parseInt(el.attr("width")) > 3;
            },
        },

    },
    canvas : {
        "tool-move" : {
            title  : "Move",
            desp : "Move an element or the entire workarea",
            icon : "move.svg",
            type : "move",
        },
        "tool-zoom" : {
            title  : "Zoom",
            desp : "Enlarge the workarea",
            icon_font : "icon-zoom-in",
            actions : ["zoom"]
        },
        "tool-light" : {
            title  : "Light",
            desp : "Highlight the labels",
            icon_font : "icon-lightbulb",
            actions : ["lightbulb"]
        }
    }
};

/**
 * Draws a featurePoint on myCanvas
 * @param {Event} position - click position
 * @param {SVGElement} container - shape that should hold the featurePoint
 * @param {DOMReact | Object} canvasOffset - offset due to canvas
 * @returns {SVGElement} SVGElement of featurePoint
 */
function getPointToDraw(position, container, canvasOffset) {
    // Get the parent svg element that surrounds the container
    var parentSvg = $('#'+container.node.id).closest('svg');
    var containerOffset = {
        x: parseInt(parentSvg.attr("x"), 10) || 0,
        y: parseInt(parentSvg.attr("y"), 10) || 0
    }
    // Feature point size should be local to each image
    var featurePointSize = labellingData[imgSelected.name].featurePointSize;
    var point =  container.parent().circle()
        .radius(Math.floor(featurePointSize))
        .attr({
            cx: position.x - canvasOffset.x - containerOffset.x,
            cy: position.y - canvasOffset.y - containerOffset.y})
        .addClass('labelpoint');
    // Set feature point colors with appConfig.featurePointColor
    $('.labelpoint').css('fill', appConfig.featurePointColor);
    point.draggable();
    return point;
}
var imgSelected = "";
var selectedElements = [];
var copiedElements;
var selectedTool = null, selectedElement = null;
var alreadyDrawing = false;
var eventBus; // Event bus used to propogate changes between tags

var plugins = {
    "facepp" : {
        title: "Face Plus Plus",
        tagName: 'facepp'
    }
}
var pluginsStore = {
    "facepp" : {
    }
}

var suggestedCategories = ["dog", "cat", "car", "vehicle", "truck", "animal", "building", "person"];
var suggestedTags = [];
var suggestedAttributes = {
    "gender" : ["male", "female", "other"],
    "color" : ["red", "green", "blue", "orange", "yellow", "white", "black"],
};
