package com.github.mercurievv.swagger3.codegen.scala.http4s;

import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.handlebars.ExtensionHelper;
import io.swagger.codegen.v3.generators.scala.AkkaHttpServerCodegen;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: Victor Mercurievv
 * Date: 4/20/2017
 * Time: 5:19 PM
 * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
 */
public class Http4sCodegen extends AkkaHttpServerCodegen {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http4sCodegen.class);

    private static final String TYPE_PREFIX = "type-";
    private static final String NEWTYPE_PREFIX = "newtype-";
    private static final String TAGGEDPE_PREFIX = "tagged-";


    @Override
    public String getDefaultTemplateDir() {
        return "scala/http4s-server";
    }

    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    public String getName() {
        return "scala-htttp4s-server";
    }

    public String getHelp() {
        return "Generates an http4s http server in scala";
    }

/*
    @Override
    public void processModelEnums(Map<String, Object> objs) {
        System.out.println("objs = " + ((CodegenModel) ((Map<String, Object>) objs.get("models")).get("model")).allowableValues);
        super.processModelEnums(objs);
    }
*/
public Map<String, Object> postProcessModels(Map<String, Object> objs) {
    List<Map<String, String>> recursiveImports = (List)objs.get("imports");
    if (recursiveImports == null) {
        return objs;
    } else {
        ListIterator listIterator = recursiveImports.listIterator();

        while(listIterator.hasNext()) {
            String _import = (String)((Map)listIterator.next()).get("import");
            if (this.importMapping.containsKey(_import)) {
                Map<String, String> newImportMap = new HashMap();
                newImportMap.put("import", (String)this.importMapping.get(_import));
                listIterator.add(newImportMap);
            }
        }

        return this.postProcessModelsEnum(objs);
    }
}

    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        List<Map<String, String>> imports = (List)objs.get("imports");
        List<Object> models = (List)objs.get("models");
        Iterator var4 = models.iterator();

        while(var4.hasNext()) {
            Object _mo = var4.next();
            Map<String, Object> mo = (Map)_mo;
            CodegenModel cm = (CodegenModel)mo.get("model");
            boolean isEnum = ExtensionHelper.getBooleanValue(cm, "x-is-enum");
            if (Boolean.TRUE.equals(isEnum) && cm.allowableValues != null) {
//                cm.imports.add((String)this.importMapping.get("JsonValue"));
                Map<String, String> item = new HashMap();
                item.put("import", (String)this.importMapping.get("JsonValue"));
                imports.add(item);
            }
        }

        return objs;
    }


    @Override
    public CodegenOperation fromOperation(String ppath, String httpMethod, Operation operation, Map<String, Schema> schemas, OpenAPI openAPI) {
        CodegenOperation op = super.fromOperation(ppath, httpMethod, operation, schemas, openAPI);
//        op.vendorExtensions.put("x-enumImports", enumImports);//.stream().collect(Collectors.joining("", "import " + modelPackage + ".", "\\n")));

        List<String> path = pathToHttp4sRoute(op.path);

        List<String> type = pathToClientType(op.path, op.pathParams);
        String pathStringClient = path.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(" / "));
        String pathStringServer = processParams(operation, pathStringClient, op.queryParams);
        op.allParams
                .forEach(p -> {
                    System.out.println("p.dataFormat = " + p.paramName + " " + p.dataFormat);
                    p.datatypeWithEnum = p.dataType;
                    if (p.dataFormat != null) {
                        if (p.dataFormat.startsWith(TYPE_PREFIX)) {
                            final String newtypeName = p.dataFormat.substring(TYPE_PREFIX.length(), p.dataFormat.length());
                            p.vendorExtensions.put("typeName", newtypeName);
                        }
                    }
                });
//        String pathStringClient = processParamsClient(pathStringClient, op.queryParams);
//        pathStringServer = processParams(operation, pathStringServer, op.headerParams);

        // Either body or form data parameters appended to route
        // As far as I know, you cannot have two ReqBody routes.
        // Is it possible to have body params AND have form params?
        String bodyType = null;
        if (op.getHasBodyParam()) {
            for (CodegenParameter param : op.bodyParams) {
                path.add("ReqBody '[JSON] " + param.dataType);
                bodyType = param.dataType;
            }
        } else if (op.getHasFormParams()) {
            // Use the FormX data type, where X is the conglomerate of all things being passed
            String formName = "Form" + camelize(op.operationId);
            bodyType = formName;
            path.add("ReqBody '[FormUrlEncoded] " + formName);
        }
        if (bodyType != null) {
            type.add(bodyType);
        }

        // Special headers appended to route
        for (CodegenParameter param : op.headerParams) {
            path.add("Header \"" + param.baseName + "\" " + param.dataType);

            String paramType = param.dataType;
            if (param.getIsListContainer()) {
                paramType = makeQueryListType(paramType, param.collectionFormat);
            }
            type.add("Maybe " + paramType);
        }
        op.vendorExtensions.put("x-routeType", pathStringServer);

        op.vendorExtensions.put("x-routeTypeClient", pathStringClient);

        op.allParams.forEach(this::addTypesafityToParam);

        return op;
    }




    private String processParams(Operation operation, String pathString, final List<CodegenParameter> params) {
        // Query parameters appended to routes
        if (!params.isEmpty()) {
            String queryParams = params.stream()
                    .filter(codegenParameter -> !codegenParameter.baseName.isEmpty())
                    .map(codegenParameter -> "Decoder_" + operation.getOperationId() + "_" + codegenParameter.baseName + "(" + codegenParameter.baseName + "D)")
                    .collect(Collectors.joining(" +& ", " :? ", ""));
            System.out.println("queryParams = " + queryParams);
            pathString = pathString + queryParams;
        }
        return pathString;
    }

    private void addTypesafityToParam(CodegenParameter cp) {
        if (cp.dataFormat != null && cp.dataFormat.startsWith(NEWTYPE_PREFIX)) {
            toApiImport("import shapeless.tag.@@");
            toApiImport("import shapeless.tag");

            final String newtypeName = cp.dataFormat.substring(NEWTYPE_PREFIX.length(), cp.dataFormat.length());
            cp.vendorExtensions.put("newtypeName", newtypeName);
        }
        if (cp.dataFormat != null && cp.dataFormat.startsWith(TAGGEDPE_PREFIX)) {
            toApiImport("import shapeless.tag.@@");
            toApiImport("import shapeless.tag");

            final String tagName = cp.dataFormat.substring(TAGGEDPE_PREFIX.length(), cp.dataFormat.length());
            cp.vendorExtensions.put("tagName", tagName);
        }
    }

    private List<String> pathToHttp4sRoute(String path) {
        // Map the capture params by their names.

        // Cut off the leading slash, if it is present.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }


        // Convert the path into a list of servant route components.
        List<String> pathComponents = new ArrayList<>();
        for (String piece : path.split("/")) {
            if (piece.startsWith("{") && piece.endsWith("}")) {
                String name = piece.substring(1, piece.length() - 1);
                pathComponents.add(" " + name + " ");
            } else {
                pathComponents.add("\"" + piece + "\"");
            }

        }


        // Intersperse the servant route pieces with :> to construct the final API type
        return pathComponents;
    }

    private List<String> pathToClientType(String path, List<CodegenParameter> pathParams) {
        // Map the capture params by their names.
        HashMap<String, String> captureTypes = new HashMap<>();
        for (CodegenParameter param : pathParams) {
            captureTypes.put(param.baseName, param.dataType);
        }

        // Cut off the leading slash, if it is present.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Convert the path into a list of servant route components.
        List<String> type = new ArrayList<>();
        for (String piece : path.split("/")) {
            if (piece.startsWith("{") && piece.endsWith("}")) {
                String name = piece.substring(1, piece.length() - 1);
                type.add(captureTypes.get(name));
            }
        }

        return type;
    }

    private String makeQueryListType(String type, String collectionFormat) {
        type = type.substring(1, type.length() - 1);
        switch (collectionFormat) {
            case "csv":
                return "(QueryList 'CommaSeparated (" + type + "))";
            case "tsv":
                return "(QueryList 'TabSeparated (" + type + "))";
            case "ssv":
                return "(QueryList 'SpaceSeparated (" + type + "))";
            case "pipes":
                return "(QueryList 'PipeSeparated (" + type + "))";
            case "multi":
                return "(QueryList 'MultiParamArray (" + type + "))";
            default:
                throw new UnsupportedOperationException();
        }
    }

}
