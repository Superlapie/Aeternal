package com.cacheextractor.exporters;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.models.ModelType;

/**
 * Exports model definitions from the OSRS cache to JSON format.
 * Includes model metadata and geometry information for RSPS development.
 */
public class ModelExporter extends JSONExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelExporter.class);
    
    public ModelExporter(ExtractionConfig config) {
        super(config);
    }
    
    /**
     * Exports all model definitions to JSON
     */
    public boolean export(Cache cache) {
        try {
            logger.info("Starting model export...");
            
            TypeList<ModelType> modelTypes = cache.getTypeList(ModelType.class);
            JsonObject root = new JsonObject();
            JsonArray models = new JsonArray();
            
            int count = 0;
            for (ModelType modelType : modelTypes) {
                if (modelType != null) {
                    JsonObject modelJson = serializeModel(modelType);
                    models.add(modelJson);
                    count++;
                }
            }
            
            root.add("models", models);
            root.add("metadata", createMetadataJson("models", count));
            
            Path outputPath = getOutputPath("models");
            writeJsonToFile(root, outputPath);
            
            logger.info("Exported {} models to {}", count, outputPath);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to export models: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Serializes a ModelType to JSON
     */
    private JsonObject serializeModel(ModelType model) {
        JsonObject json = new JsonObject();
        
        // Basic information
        json.addProperty("id", model.id);
        
        // Vertex counts
        json.addProperty("vertexCount", model.vertexCount);
        json.addProperty("triangleCount", model.triangleCount);
        
        // Texture coordinates
        if (model.textureCoordinates != null) {
            JsonArray texCoords = new JsonArray();
            for (short coord : model.textureCoordinates) {
                texCoords.add(coord);
            }
            json.add("textureCoordinates", texCoords);
        }
        
        // Texture triangle faces
        if (model.textureTriangleFaces != null) {
            JsonArray texFaces = new JsonArray();
            for (short face : model.textureTriangleFaces) {
                texFaces.add(face);
            }
            json.add("textureTriangleFaces", texFaces);
        }
        
        // Vertex positions
        if (model.vertexPositionsX != null) {
            JsonArray positionsX = new JsonArray();
            for (int pos : model.vertexPositionsX) {
                positionsX.add(pos);
            }
            json.add("vertexPositionsX", positionsX);
        }
        
        if (model.vertexPositionsY != null) {
            JsonArray positionsY = new JsonArray();
            for (int pos : model.vertexPositionsY) {
                positionsY.add(pos);
            }
            json.add("vertexPositionsY", positionsY);
        }
        
        if (model.vertexPositionsZ != null) {
            JsonArray positionsZ = new JsonArray();
            for (int pos : model.vertexPositionsZ) {
                positionsZ.add(pos);
            }
            json.add("vertexPositionsZ", positionsZ);
        }
        
        // Triangle faces
        if (model.triangleFacesX != null) {
            JsonArray facesX = new JsonArray();
            for (short face : model.triangleFacesX) {
                facesX.add(face);
            }
            json.add("triangleFacesX", facesX);
        }
        
        if (model.triangleFacesY != null) {
            JsonArray facesY = new JsonArray();
            for (short face : model.triangleFacesY) {
                facesY.add(face);
            }
            json.add("triangleFacesY", facesY);
        }
        
        if (model.triangleFacesZ != null) {
            JsonArray facesZ = new JsonArray();
            for (short face : model.triangleFacesZ) {
                facesZ.add(face);
            }
            json.add("triangleFacesZ", facesZ);
        }
        
        // Vertex normals
        if (model.vertexNormalsX != null) {
            JsonArray normalsX = new JsonArray();
            for (int normal : model.vertexNormalsX) {
                normalsX.add(normal);
            }
            json.add("vertexNormalsX", normalsX);
        }
        
        if (model.vertexNormalsY != null) {
            JsonArray normalsY = new JsonArray();
            for (int normal : model.vertexNormalsY) {
                normalsY.add(normal);
            }
            json.add("vertexNormalsY", normalsY);
        }
        
        if (model.vertexNormalsZ != null) {
            JsonArray normalsZ = new JsonArray();
            for (int normal : model.vertexNormalsZ) {
                normalsZ.add(normal);
            }
            json.add("vertexNormalsZ", normalsZ);
        }
        
        // Colors
        if (model.colors != null) {
            JsonArray colors = new JsonArray();
            for (short color : model.colors) {
                colors.add(color);
            }
            json.add("colors", colors);
        }
        
        // Priorities
        if (model.priorities != null) {
            JsonArray priorities = new JsonArray();
            for (byte priority : model.priorities) {
                priorities.add(priority);
            }
            json.add("priorities", priorities);
        }
        
        // Transparency
        if (model.transparency != null) {
            JsonArray transparency = new JsonArray();
            for (byte trans : model.transparency) {
                transparency.add(trans);
            }
            json.add("transparency", transparency);
        }
        
        // Texture IDs
        if (model.textureIds != null) {
            JsonArray textureIds = new JsonArray();
            for (short textureId : model.textureIds) {
                textureIds.add(textureId);
            }
            json.add("textureIds", textureIds);
        }
        
        // Texture usage
        if (model.textureUses != null) {
            JsonArray textureUses = new JsonArray();
            for (byte use : model.textureUses) {
                textureUses.add(use);
            }
            json.add("textureUses", textureUses);
        }
        
        // Bounds
        json.addProperty("boundsHeight", model.boundsHeight);
        json.addProperty("boundsX1", model.boundsX1);
        json.addProperty("boundsX2", model.boundsX2);
        json.addProperty("boundsY1", model.boundsY1);
        json.addProperty("boundsY2", model.boundsY2);
        json.addProperty("boundsZ1", model.boundsZ1);
        json.addProperty("boundsZ2", model.boundsZ2);
        
        // Flags
        json.addProperty("hasAlpha", model.hasAlpha);
        json.addProperty("hasTextureTriangleFaces", model.hasTextureTriangleFaces);
        json.addProperty("hasTextureCoordinates", model.hasTextureCoordinates);
        json.addProperty("hasVertexNormals", model.hasVertexNormals);
        
        return json;
    }
}
