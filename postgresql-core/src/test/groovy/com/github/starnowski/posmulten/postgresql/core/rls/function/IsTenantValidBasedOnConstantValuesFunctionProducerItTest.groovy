package com.github.starnowski.posmulten.postgresql.core.rls.function

import com.github.starnowski.posmulten.postgresql.core.TestApplication
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

import static com.github.starnowski.posmulten.postgresql.core.common.function.FunctionArgumentValue.forNumeric
import static com.github.starnowski.posmulten.postgresql.core.common.function.FunctionArgumentValue.forString

@SpringBootTest(classes = [TestApplication.class])
class IsTenantValidBasedOnConstantValuesFunctionProducerItTest extends Specification {

    @Autowired
    JdbcTemplate jdbcTemplate

    def tested = new IsTenantValidBasedOnConstantValuesFunctionProducer()

    def functionName
    def schema
    def functionDefinition

    @Unroll
    def "should generate statement that creates function '#testFunctionName' for schema '#testSchema' with argument type '#argumentType' (null means 'text') with invalid tenant values'#invalidTenantValues'" () {
        expect:
            tested.produce(new IsTenantValidBasedOnConstantValuesFunctionProducerParameters(testFunctionName, testSchema, new HashSet<String>(invalidTenantValues), argumentType)).getCreateScript() == expectedStatement

        where:
            testSchema              |   testFunctionName            |   invalidTenantValues                         |   argumentType                ||  invalidValues                                               |   validValues
            null                    |   "is_tenant_valid"           |   ["3325", "adfzxcvz"]                        |   null                        ||  [forString("3325"), forString("adfzxcvz")]                  |   [forString("afdxzv"), forString("1234asdf")]
            "public"                |   "tenant_is_valid"           |   ["dfgxcbx", "ZCVZSFDA"]                     |   null                        ||  [forString("ZCVZSFDA"), forString("dfgxcbx")]               |   [forString("1324ASF"), forString("SOME VALUE")]
            "non_public_schema"     |   "validtenant"               |   ["zvcz", "33asdfcxvzv"]                     |   null                        ||  [forString("33asdfcxvzv"), forString("zvcz")]               |   [forString("SomeValues"), forString("TEnant1")]
            "public"                |   "valid_tenant"              |   ["dgfsg", "433"]                            |   "VARCHAR(32)"               ||  [forString("dgfsg"), forString("433")]                      |   [forString("tenat2"), forString("TEnantXXXXX")]
            "non_public_schema"     |   "tenant_is_correct"         |   ["66", "12", "0"]                           |   "INTEGER"                   ||  [forNumeric("66"), forNumeric("12"), forNumeric("0")]       |   [forNumeric("1000"), forNumeric("4")]
            "public"                |   "valid_tenant"              |   ["dgfsg", "433"]                            |   "character varying(255)"    ||  [forString("dgfsg"), forString("433")]                      |   [forString("tenat2"), forString("TEnantXXXXX")]
            "non_public_schema"     |   "tenant_uuid_is_valid"      |   ["40e6215d-b5c6-4896-987c-f30f3678f608"]    |   "uuid"                      ||  [forString("40e6215d-b5c6-4896-987c-f30f3678f608")]         |   [forString("40e6215d-b5c6-4896-987c-f30f3678f608")]
    }

    def returnSelectStatementAsBoolean(String selectStatement)
    {
        return jdbcTemplate.execute(new StatementCallback<Boolean>() {
            @Override
            Boolean doInStatement(Statement statement) throws SQLException, DataAccessException {
                ResultSet rs = statement.executeQuery(selectStatement)
                rs.next()
                return rs.getBoolean(1)
            }
        })
    }
}
