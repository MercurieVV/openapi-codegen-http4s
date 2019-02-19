package com.github.mercurievv.swagger3.codegen.scala.http4s;

import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.scala.AkkaHttpServerCodegen;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
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
        return "scala/http4s-server2";
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

    @Override
    public void processOpts() {
        super.processOpts();

        importMapping.clear();

//        instantiationTypes.put("array", "List");
        instantiationTypes.put("map", "Map");

//        typeMapping.put("FeatureTestResult", "FeatureTestResult.FeatureTestResult");
        importMapping.remove("ApiModelProperty");
        importMapping.remove("ApiModel");

        importMapping.remove("List");

        importMapping.put("JsonValue", "scala.Int");

        typeMapping.put("array", "List");
        typeMapping.put("map", "Map");

        typeMapping.put("integer", "Int");
        importMapping.put("Int", "scala.Int");

        typeMapping.put("date", "LocalDate");
        importMapping.put("LocalDate", "java.time.LocalDate");

        typeMapping.put("DateTime", "ZonedDateTime");
        importMapping.put("ZonedDateTime", "java.time.ZonedDateTime");

        importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");

        importMapping.put("BigDecimal", "scala.BigDecimal");
    }

    @Override
    public CodegenParameter fromRequestBody(RequestBody body, String name, Schema schema, Map<String, Schema> schemas, Set<String> imports) {
        final CodegenParameter codegenParameter = super.fromRequestBody(body, name, schema, schemas, imports);
        if (body.getExtensions() != null && body.getExtensions().get("x-varname") != null) {
            final String bodyName = (String) body.getExtensions().get("x-varname");
            codegenParameter.baseName = bodyName;
            codegenParameter.paramName = bodyName;
        }
        if(schema == null)
            schema = this.getSchemaFromBody(body);
        if(schema.getEnum() != null && !schema.getEnum().isEmpty()){
            codegenParameter.baseType = name;
            codegenParameter.dataType = name;
            //codegenParameter.datatypeWithEnum = name;
        }
        if(codegenParameter.datatypeWithEnum == null)
            codegenParameter.datatypeWithEnum = codegenParameter.dataType;
        return codegenParameter;
    }

    public String getSchemaType(Schema schema) {
        String schemaType = super.getSchemaType(schema);
        schemaType = this.getAlias(schemaType);
        if (this.typeMapping.containsKey(schemaType)) {
            return this.typeMapping.get(schemaType);
        } else {
            if (null == schemaType && schema.getName() != null) {
                LOGGER.warn("No Type defined for Property " + schema.getName());
            }

            return this.toModelName(schemaType);
        }
    }
    public String getTypeDeclaration(Schema propertySchema) {
        Schema inner;
        if (propertySchema instanceof ArraySchema) {
            inner = ((ArraySchema)propertySchema).getItems();
            return String.format("%s[%s]", this.getSchemaType(propertySchema), this.getTypeDeclaration(inner));
        } else if (propertySchema instanceof ObjectSchema && hasSchemaProperties(propertySchema)) {
            inner = (Schema)propertySchema.getAdditionalProperties();
            return String.format("%s[String, %s]", this.getSchemaType(propertySchema), this.getTypeDeclaration(inner));
        } else {
            return super.getTypeDeclaration(propertySchema);
        }
    }

    public String toModelName(String name) {
        return camelize(this.sanitizeName(name));
    }

    public String toEnumName(CodegenProperty property) {
        return this.sanitizeName(camelize(property.name)) + "Enum";
    }

    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        final Map<String, Object> objectMap = super.postProcessModels(objs);
        return this.postProcessModelsEnum(objectMap);

    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> stringObjectMap = super.postProcessOperations(objs);

        Map<String, Object> operations = (Map<String, Object>) stringObjectMap.get("operations");
        if (operations == null) {
            return stringObjectMap;
        }
        List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation operation : ops) {
            if (operation.returnType == null) {
                operation.returnType = "Unit";
            } else if (operation.returnType.startsWith("List")) {
                String rt = operation.returnType;
                int end = rt.lastIndexOf("]");
                if (end > 0) {
                    operation.returnType = rt.substring("List[".length(), end).trim();
                    operation.returnContainer = "List";
                    String varName = operation.returnType.substring(0, 1).toLowerCase() + operation.returnType.substring(1) + "s";
                    operation.vendorExtensions.put("returnTypeVar", toVarName(varName));
                }
            }
        }
        return stringObjectMap;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        String varName = toEnumVarNameNoCasing(value, datatype);
        if (varName.equals("new")) varName = "`new`";
        return varName;
    }


    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        final CodegenParameter p  = super.fromParameter(parameter, imports);
        if(p.datatypeWithEnum == null)
            p.datatypeWithEnum = p.dataType;
        if (p.dataFormat != null) {
            if (p.dataFormat.startsWith(TYPE_PREFIX)) {
                final String newtypeName = p.dataFormat.substring(TYPE_PREFIX.length());
                p.vendorExtensions.put("typeName", newtypeName);
            }
        }
        return p;
    }

    @Override
    public CodegenProperty fromProperty(String name, Schema p) {
        name = sanitieProperty(name);
        CodegenProperty codegenProperty = super.fromProperty(name, p);
        List<String> predicates = new ArrayList<>();
        if (codegenProperty.minimum != null) {
            predicates.add("");
            if (codegenProperty.minimum.equals("0")) {
                predicates.add("NonNegative");
                codegenProperty.minimum = null;
            }
        }
        if (codegenProperty.maximum != null) {
            predicates.add("");
            if (codegenProperty.maximum.equals("0")) {
                predicates.add("NonPositive");
                codegenProperty.maximum = null;
            }
        }
        if (p.getFormat() != null) {
            if (p.getFormat().startsWith(NEWTYPE_PREFIX)) {
                toModelImport("import shapeless.tag.@@");
                toApiImport("import shapeless.tag.@@");
                toApiImport("import shapeless.tag");

                final String newtypeName = p.getFormat().substring(NEWTYPE_PREFIX.length());
                codegenProperty.vendorExtensions.put("newtypeName", newtypeName);
            }
        }
        if (p.getFormat() != null) {
            if (p.getFormat().startsWith(TYPE_PREFIX)) {
                final String newtypeName = p.getFormat().substring(TYPE_PREFIX.length());
                codegenProperty.vendorExtensions.put("typeName", newtypeName);
            }
        }
        if (!predicates.isEmpty()) {
            codegenProperty.vendorExtensions.put("predicates", predicates.stream().collect(Collectors.joining(" ")));
            toModelImport("import eu.timepit.refined._");
            toModelImport("import eu.timepit.refined.api.Refined");
            toModelImport("import eu.timepit.refined.auto._");
            toModelImport("import eu.timepit.refined.numeric._");
            toModelImport("import co.nextwireless.newtype.tags._");
            toApiImport("import eu.timepit.refined._");
            toApiImport("import eu.timepit.refined.api.Refined");
            toApiImport("import eu.timepit.refined.auto._");
            toApiImport("import eu.timepit.refined.numeric._");
            toApiImport("import co.nextwireless.newtype.tags._");
        }
//        if (codegenProperty.isEnum)
//            enumImports.add(codegenProperty.datatypeWithEnum);
        return codegenProperty;
    }

    private String sanitieProperty(String name) {
        if(name.contains("-") && !name.startsWith("`"))
            return "`" + name + "`";
        return name;
    }


    @Override
    public CodegenOperation fromOperation(String ppath, String httpMethod, Operation operation, Map<String, Schema> schemas, OpenAPI openAPI) {
        CodegenOperation op = super.fromOperation(ppath, httpMethod, operation, schemas, openAPI);
//        op.vendorExtensions.put("x-enumImports", enumImports);//.stream().collect(Collectors.joining("", "import " + modelPackage + ".", "\\n")));

        http4sCode(operation, op);

        op.allParams.sort(Comparator.comparingInt(this::order));

        op.allParams.forEach(this::addTypesafityToParam);
//        addHasMore((List)op.bodyParams);
//        addHasMore((List)op.pathParams);
//        addHasMore((List)op.queryParams);
//        addHasMore((List)op.headerParams);
//        addHasMore((List)op.formParams);
//        addHasMore((List)op.requiredParams);
        addHasMore((List) op.allParams);

        return op;
    }

    private void http4sCode(Operation operation, CodegenOperation op) {
        List<String> path = pathToHttp4sRoute(op.path);

        List<String> type = pathToClientType(op.path, op.pathParams);
        String pathStringClient = path.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(" / "));
        String pathStringServer = processParams(operation, pathStringClient, op.queryParams);
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
    }

    private int order(CodegenParameter codegenParameter) {
        if (codegenParameter.getIsHeaderParam())
            return 1;
        if (codegenParameter.getIsCookieParam())
            return 2;
        if (codegenParameter.getIsPathParam())
            return 3;
        if (codegenParameter.getIsQueryParam())
            return 4;
        if (codegenParameter.getIsFormParam())
            return 5;
        if (codegenParameter.getIsBodyParam())
            return 6;
        return 7;
    }

    private String toEnumVarNameNoCasing(String value, String datatype) {
        if (value.length() == 0) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(value) != null) {
            return getSymbolName(value);
        }

        // number
        if ("Integer".equals(datatype) || "Long".equals(datatype) ||
                "Float".equals(datatype) || "Double".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String var = value.replaceAll("\\W+", "_");
        if (var.matches("\\d.*")) {
            return "_" + var;
        } else {
            return var;
        }
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

    private static List<CodegenParameter> addHasMore(List<CodegenParameter> objs) {
        if (objs != null) {
            for (int i = 0; i < objs.size(); ++i) {
                objs.get(i).secondaryParam = i > 0;
                objs.get(i).getVendorExtensions().put("x-has-more", i < objs.size() - 1);
                System.out.println("i = " + i + " " + objs.get(i).secondaryParam + " " + (objs.size() - 1) + " " + objs.get(i).paramName);
            }
        }

        return objs;
    }

    private void addTypesafityToParam(CodegenParameter cp) {
        if (cp.dataFormat != null && cp.dataFormat.startsWith(NEWTYPE_PREFIX)) {
            toApiImport("import shapeless.tag.@@");
            toApiImport("import shapeless.tag");

            final String newtypeName = cp.dataFormat.substring(NEWTYPE_PREFIX.length());
            cp.vendorExtensions.put("newtypeName", newtypeName);
        }
        if (cp.dataFormat != null && cp.dataFormat.startsWith(TAGGEDPE_PREFIX)) {
            toApiImport("import shapeless.tag.@@");
            toApiImport("import shapeless.tag");

            final String tagName = cp.dataFormat.substring(TAGGEDPE_PREFIX.length());
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
