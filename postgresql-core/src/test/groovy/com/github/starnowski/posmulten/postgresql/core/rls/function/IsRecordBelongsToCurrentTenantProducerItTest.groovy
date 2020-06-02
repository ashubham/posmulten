package com.github.starnowski.posmulten.postgresql.core.rls.function

import com.github.starnowski.posmulten.postgresql.core.TestApplication
import com.github.starnowski.posmulten.postgresql.core.common.function.FunctionArgumentValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.StatementCallback
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

import static com.github.starnowski.posmulten.postgresql.core.TestUtils.isFunctionExists
import static com.github.starnowski.posmulten.postgresql.core.common.function.FunctionArgumentValue.forNumeric
import static com.github.starnowski.posmulten.postgresql.core.rls.function.AbstractIsRecordBelongsToCurrentTenantProducerParameters.pairOfColumnWithType
import static org.junit.Assert.assertEquals

@SpringBootTest(classes = [TestApplication.class])
class IsRecordBelongsToCurrentTenantProducerItTest extends Specification {

    private static String VALID_CURRENT_TENANT_ID_PROPERTY_NAME = "c.c_ten"
    IGetCurrentTenantIdFunctionInvocationFactory getCurrentTenantIdFunctionInvocationFactory =
            {
                "current_setting('" + VALID_CURRENT_TENANT_ID_PROPERTY_NAME + "')"
            }
    String schema
    String functionName
    def tested = new IsRecordBelongsToCurrentTenantProducer()
    def functionDefinition

    @Autowired
    JdbcTemplate jdbcTemplate

    @Unroll
    def "for function name '#testFunctionName' for schema '#testSchema', table #recordTableName in schema #recordSchemaName that compares values for columns #keyColumnsPairs and tenant column #tenantColumnPair, should generate statement that creates function" () {
        given:
            functionName = testFunctionName
            schema = testSchema
            assertEquals(false, isFunctionExists(jdbcTemplate, functionName, schema))
            def parameters = new IsRecordBelongsToCurrentTenantProducerParameters.Builder()
                    .withSchema(testSchema)
                    .withFunctionName(testFunctionName)
                    .withRecordTableName(recordTableName)
                    .withRecordSchemaName(recordSchemaName)
                    .withiGetCurrentTenantIdFunctionInvocationFactory(getCurrentTenantIdFunctionInvocationFactory)
                    .withTenantColumnPair(tenantColumnPair)
                    .withKeyColumnsPairsList(keyColumnsPairs).build()

        when:
            functionDefinition = tested.produce(parameters)
            jdbcTemplate.execute(functionDefinition.getCreateScript())

        then:
            isFunctionExists(jdbcTemplate, functionName, schema)

        where:
            testSchema              |   testFunctionName                        |   recordTableName     |   recordSchemaName    |   tenantColumnPair                                            |   keyColumnsPairs
            null                    |   "is_user_belongs_to_current_tenant"     |   "users"             |   null                |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]
            "public"                |   "is_user_belongs_to_current_tenant"     |   "users"             |   null                |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]
            "public"                |   "is_user_belongs_to_current_tenant"     |   "users"             |   "public"            |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]
            "non_public_schema"     |   "is_user_belongs_to_current_tenant"     |   "users"             |   null                |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]
            "non_public_schema"     |   "is_user_belongs_to_current_tenant"     |   "users"             |   "public"            |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]
            "non_public_schema"     |   "is_user_belongs_to_current_tenant"     |   "users"             |   "non_public_schema" |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]
            "public"                |   "is_comments_belongs_to_current_tenant" |   "comments"          |   "public"            |   pairOfColumnWithType("tenant", "character varying(255)")    |   [pairOfColumnWithType("id", "int"), pairOfColumnWithType("user_id", "bigint")]
            "non_public_schema"     |   "is_comments_belongs_to_current_tenant" |   "comments"          |   "public"            |   pairOfColumnWithType("tenant", "character varying(255)")    |   [pairOfColumnWithType("id", "int"), pairOfColumnWithType("user_id", "bigint")]
            "non_public_schema"     |   "is_comments_belongs_to_current_tenant" |   "comments"          |   "non_public_schema" |   pairOfColumnWithType("tenant", "character varying(255)")    |   [pairOfColumnWithType("id", "int"), pairOfColumnWithType("user_id", "bigint")]
    }

    @Unroll
    def "for table #recordTableName in schema #recordSchemaName that compares values for columns #keyColumnsPairs and tenant column #tenantColumnPair, should return positve boolean result" () {
        given:
            functionName = "is_user_belongs_to_current_tenant"
            schema = "public"
            assertEquals(false, isFunctionExists(jdbcTemplate, functionName, schema))
            def parameters = new IsRecordBelongsToCurrentTenantProducerParameters.Builder()
                    .withSchema(schema)
                    .withFunctionName(functionName)
                    .withRecordTableName(recordTableName)
                    .withRecordSchemaName(recordSchemaName)
                    .withiGetCurrentTenantIdFunctionInvocationFactory(getCurrentTenantIdFunctionInvocationFactory)
                    .withTenantColumnPair(tenantColumnPair)
                    .withKeyColumnsPairsList(keyColumnsPairs).build()
            Map<String, FunctionArgumentValue> map = new HashMap<>();
            map.put(tenantColumnPair.getKey(), testCurrentTenantIdValue)

        when:
            functionDefinition = tested.produce(parameters)
            jdbcTemplate.execute(functionDefinition.getCreateScript())

        then:
            getBooleanResultForSelectStatement(testCurrentTenantIdValue, returnTestedSelectStatement(functionDefinition.returnIsRecordBelongsToCurrentTenantFunctionInvocation(forNumeric(String.valueOf(testUsersId)), map))) == expectedBooleanValue

        where:
        recordTableName     |   recordSchemaName    |   tenantColumnPair                                            |   keyColumnsPairs                         |   testCurrentTenantIdValue    |   testUsersId || expectedBooleanValue
        "users"             |   null                |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]  |   "primary_tenant"            |   1           ||  true
        "users"             |   null                |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]  |   "primary_tenant"            |   1           ||  true
        "users"             |   "public"            |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]  |   "primary_tenant"            |   1           ||  true
        "users"             |   null                |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]  |   "primary_tenant"            |   1           ||  true
        "users"             |   "public"            |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]  |   "primary_tenant"            |   1           ||  true
        "users"             |   "non_public_schema" |   pairOfColumnWithType("tenant_id", "text")                   |   [pairOfColumnWithType("id", "bigint")]  |   "primary_tenant"            |   1           ||  true
//        "comments"          |   "public"            |   pairOfColumnWithType("tenant", "character varying(255)")    |   [pairOfColumnWithType("id", "int"), pairOfColumnWithType("user_id", "bigint")]
//        "comments"          |   "public"            |   pairOfColumnWithType("tenant", "character varying(255)")    |   [pairOfColumnWithType("id", "int"), pairOfColumnWithType("user_id", "bigint")]
//        "comments"          |   "non_public_schema" |   pairOfColumnWithType("tenant", "character varying(255)")    |   [pairOfColumnWithType("id", "int"), pairOfColumnWithType("user_id", "bigint")]
    }

    def getBooleanResultForSelectStatement(String propertyValue, String selectStatement)
    {
        return jdbcTemplate.execute(new StatementCallback<Boolean>() {
            @Override
            Boolean doInStatement(Statement statement) throws SQLException, DataAccessException {
                statement.execute("SET " + VALID_CURRENT_TENANT_ID_PROPERTY_NAME + " = '" + propertyValue + "';")
                ResultSet rs = statement.executeQuery(selectStatement)
                rs.next()
                return rs.getBoolean(1)
            }
        })
    }

    def returnTestedSelectStatement(String functionIvocation)
    {
        "SELECT " + functionIvocation
    }

    def cleanup() {
        jdbcTemplate.execute(functionDefinition.getDropScript())
        assertEquals(false, isFunctionExists(jdbcTemplate, functionName, schema))
    }

}
