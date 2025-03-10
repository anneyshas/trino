/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static io.trino.client.ClientStandardTypes.ARRAY;
import static io.trino.client.ClientStandardTypes.BIGINT;
import static io.trino.client.ClientStandardTypes.BOOLEAN;
import static io.trino.client.ClientStandardTypes.CHAR;
import static io.trino.client.ClientStandardTypes.DATE;
import static io.trino.client.ClientStandardTypes.DECIMAL;
import static io.trino.client.ClientStandardTypes.DOUBLE;
import static io.trino.client.ClientStandardTypes.GEOMETRY;
import static io.trino.client.ClientStandardTypes.INTEGER;
import static io.trino.client.ClientStandardTypes.INTERVAL_DAY_TO_SECOND;
import static io.trino.client.ClientStandardTypes.INTERVAL_YEAR_TO_MONTH;
import static io.trino.client.ClientStandardTypes.IPADDRESS;
import static io.trino.client.ClientStandardTypes.JSON;
import static io.trino.client.ClientStandardTypes.MAP;
import static io.trino.client.ClientStandardTypes.REAL;
import static io.trino.client.ClientStandardTypes.ROW;
import static io.trino.client.ClientStandardTypes.SMALLINT;
import static io.trino.client.ClientStandardTypes.SPHERICAL_GEOGRAPHY;
import static io.trino.client.ClientStandardTypes.TIME;
import static io.trino.client.ClientStandardTypes.TIMESTAMP;
import static io.trino.client.ClientStandardTypes.TIMESTAMP_WITH_TIME_ZONE;
import static io.trino.client.ClientStandardTypes.TIME_WITH_TIME_ZONE;
import static io.trino.client.ClientStandardTypes.TINYINT;
import static io.trino.client.ClientStandardTypes.UUID;
import static io.trino.client.ClientStandardTypes.VARCHAR;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

public final class JsonDecodingUtils
{
    private JsonDecodingUtils() {}

    public static TypeDecoder[] createTypeDecoders(List<Column> columns)
    {
        verify(!columns.isEmpty(), "Columns must not be empty");
        return columns.stream()
                .map(column -> createTypeDecoder(column.getTypeSignature()))
                .toArray(TypeDecoder[]::new);
    }

    public interface TypeDecoder
    {
        Object decode(JsonParser parser)
                throws IOException;
    }

    private static TypeDecoder createTypeDecoder(ClientTypeSignature signature)
    {
        switch (signature.getRawType()) {
            case BIGINT:
                return new BigIntegerDecoder();
            case INTEGER:
                return new IntegerDecoder();
            case SMALLINT:
                return new SmallintDecoder();
            case TINYINT:
                return new TinyintDecoder();
            case DOUBLE:
                return new DoubleDecoder();
            case REAL:
                return new RealDecoder();
            case BOOLEAN:
                return new BooleanDecoder();
            case ARRAY:
                return new ArrayDecoder(signature);
            case MAP:
                return new MapDecoder(signature);
            case ROW:
                return new RowDecoder(signature);
            case VARCHAR:
            case JSON:
            case TIME:
            case TIME_WITH_TIME_ZONE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIME_ZONE:
            case DATE:
            case INTERVAL_YEAR_TO_MONTH:
            case INTERVAL_DAY_TO_SECOND:
            case IPADDRESS:
            case UUID:
            case DECIMAL:
            case CHAR:
            case GEOMETRY:
            case SPHERICAL_GEOGRAPHY:
                return new StringDecoder();
            default:
                return new Base64Decoder();
        }
    }

    private static class BigIntegerDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_NUMBER_INT:
                    return parser.getLongValue();
                case VALUE_STRING:
                    return Long.parseLong(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class IntegerDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_NUMBER_INT:
                    return parser.getIntValue();
                case VALUE_STRING:
                    return Integer.parseInt(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class SmallintDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_NUMBER_INT:
                    return parser.getShortValue();
                case VALUE_STRING:
                    return Short.parseShort(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class TinyintDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_NUMBER_INT:
                    return parser.getByteValue();
                case VALUE_STRING:
                    return Byte.parseByte(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class DoubleDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_NUMBER_FLOAT:
                    return parser.getDoubleValue();
                case VALUE_STRING:
                    return Double.parseDouble(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class RealDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_NUMBER_FLOAT:
                    return parser.getFloatValue();
                case VALUE_STRING:
                    return Float.parseFloat(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class BooleanDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_FALSE:
                    return false;
                case VALUE_TRUE:
                    return true;
                case VALUE_STRING:
                    return Boolean.parseBoolean(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class StringDecoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_STRING:
                    return parser.getValueAsString();
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class Base64Decoder
            implements TypeDecoder
    {
        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case VALUE_NULL:
                    return null;
                case VALUE_STRING:
                    return Base64.getDecoder().decode(parser.getValueAsString());
                default:
                    throw illegalToken(parser);
            }
        }
    }

    private static class ArrayDecoder
            implements TypeDecoder
    {
        private final TypeDecoder typeDecoder;

        public ArrayDecoder(ClientTypeSignature signature)
        {
            requireNonNull(signature, "signature is null");
            checkArgument(signature.getRawType().equals(ARRAY), "not an array type signature: %s", signature);
            this.typeDecoder = createTypeDecoder(signature.getArgumentsAsTypeSignatures().get(0));
        }

        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case START_ARRAY:
                    break;
                case VALUE_NULL:
                    return null;
                default:
                    throw illegalToken(parser);
            }
            List<Object> values = new LinkedList<>(); // nulls allowed
            while (true) {
                switch (parser.nextToken()) {
                    case END_ARRAY:
                        return unmodifiableList(values);
                    case VALUE_NULL:
                        values.add(null);
                        break;
                    default:
                        values.add(typeDecoder.decode(parser));
                }
            }
        }
    }

    private static class MapDecoder
            implements TypeDecoder
    {
        private final String keyType;
        private final TypeDecoder valueDecoder;

        public MapDecoder(ClientTypeSignature signature)
        {
            requireNonNull(signature, "signature is null");
            checkArgument(signature.getRawType().equals(MAP), "not a map type signature: %s", signature);
            this.keyType = signature.getArgumentsAsTypeSignatures().get(0).getRawType();
            this.valueDecoder = createTypeDecoder(signature.getArgumentsAsTypeSignatures().get(1));
        }

        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            Map<Object, Object> values = new HashMap<>();
            if (parser.currentToken() != START_OBJECT) {
                throw illegalToken(parser);
            }

            while (true) {
                switch (parser.nextToken()) {
                    // The original JSON encoding, always converts a key to a String to use it in the JSON object
                    case FIELD_NAME:
                        Object name = decodeKey(parser.getValueAsString());
                        if (requireNonNull(parser.nextToken()) == JsonToken.VALUE_NULL) {
                            values.put(name, null);
                        }
                        else {
                            values.put(name, valueDecoder.decode(parser)); // nulls allowed
                        }
                        break;
                    case END_OBJECT:
                        return unmodifiableMap(values);
                    default:
                        illegalToken(parser);
                }
            }
        }

        private Object decodeKey(String value)
        {
            switch (keyType) {
                case BIGINT:
                    return Long.parseLong(value);
                case INTEGER:
                    return Integer.parseInt(value);
                case SMALLINT:
                    return Short.parseShort(value);
                case TINYINT:
                    return Byte.parseByte(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case REAL:
                    return Float.parseFloat(value);
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case VARCHAR:
                case JSON:
                case TIME:
                case TIME_WITH_TIME_ZONE:
                case TIMESTAMP:
                case TIMESTAMP_WITH_TIME_ZONE:
                case DATE:
                case INTERVAL_YEAR_TO_MONTH:
                case INTERVAL_DAY_TO_SECOND:
                case IPADDRESS:
                case UUID:
                case DECIMAL:
                case CHAR:
                case GEOMETRY:
                case SPHERICAL_GEOGRAPHY:
                    return value;
                default:
                    return Base64.getDecoder().decode(value);
            }
        }
    }

    private static class RowDecoder
            implements TypeDecoder
    {
        private final TypeDecoder[] fieldDecoders;
        private final List<Optional<String>> fieldNames;

        private RowDecoder(ClientTypeSignature signature)
        {
            requireNonNull(signature, "signature is null");
            checkArgument(signature.getRawType().equals(ROW), "not a row type signature: %s", signature);
            fieldDecoders = new TypeDecoder[signature.getArguments().size()];
            ImmutableList.Builder<Optional<String>> fieldNames = ImmutableList.builderWithExpectedSize(fieldDecoders.length);

            int index = 0;
            for (ClientTypeSignatureParameter parameter : signature.getArguments()) {
                checkArgument(
                        parameter.getKind() == ClientTypeSignatureParameter.ParameterKind.NAMED_TYPE,
                        "Unexpected parameter [%s] for row type",
                        parameter);
                NamedClientTypeSignature namedTypeSignature = parameter.getNamedTypeSignature();
                fieldDecoders[index] = createTypeDecoder(namedTypeSignature.getTypeSignature());
                fieldNames.add(namedTypeSignature.getName());
                index++;
            }
            this.fieldNames = fieldNames.build();
        }

        @Override
        public Object decode(JsonParser parser)
                throws IOException
        {
            switch (parser.currentToken()) {
                case START_ARRAY:
                    break;
                case VALUE_NULL:
                    return null;
                default:
                    throw illegalToken(parser);
            }
            Row.Builder row = Row.builderWithExpectedSize(fieldDecoders.length);
            for (int i = 0; i < fieldDecoders.length; i++) {
                if (requireNonNull(parser.nextToken()) == JsonToken.VALUE_NULL) {
                    row.addField(fieldNames.get(i), null);
                }
                else {
                    row.addField(fieldNames.get(i), fieldDecoders[i].decode(parser));
                }
            }
            verify(parser.nextToken() == END_ARRAY, "Expected end object, but got %s", parser.currentToken());
            return row.build();
        }
    }

    private static IllegalTokenException illegalToken(JsonParser parser)
    {
        throw new IllegalTokenException(parser);
    }

    private static class IllegalTokenException
            extends RuntimeException
    {
        public IllegalTokenException(JsonParser parser)
        {
            super(format("Unexpected token %s [location: %s]", parser.currentToken(), parser.currentLocation()));
        }
    }
}
