package com.github.starnowski.posmulten.postgresql.core.rls

import com.github.starnowski.posmulten.postgresql.core.TestApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.starnowski.posmulten.postgresql.test.utils.TestUtils.isAnyRecordExists
import static org.junit.Assert.assertEquals

@SpringBootTest(classes = [TestApplication.class])
abstract class AbstractConstraintProducerGenericItTest<X extends IConstraintProducerParameters, P extends AbstractConstraintProducer<X>> extends Specification {

    @Autowired
    JdbcTemplate jdbcTemplate

    def testSchema
    def testTable
    def testConstraint

    @Unroll
    def "should create correct the creation statements for schema #schema and table #table and constraint #constraintName"()
    {
        given:
            P tested = returnTestedObject()
            X parameters = returnCorrectParametersMockObject()
            parameters.getTableSchema() >> schema
            parameters.getTableName() >> table
            parameters.getConstraintName() >> constraintName
            testSchema = schema
            testTable = table
            testConstraint = constraintName
            def functionDefinition = tested.produce(parameters)
            assertEquals(false, isAnyRecordExists(jdbcTemplate, createSelectStatement(schema, table, constraintName)))

        when:
            jdbcTemplate.execute(functionDefinition.getCreateScript())

        then:
            assertEquals(true, isAnyRecordExists(jdbcTemplate, createSelectStatement(schema, table, constraintName)))

        where:
            schema                      |   table       |   constraintName
            null                        |   "users"     |   "con1"
            "public"                    |   "users"     |   "con1"
            "non_public_schema"         |   "users"     |   "con1"
            null                        |   "posts"     |   "con1"
            "public"                    |   "posts"     |   "con1"
            "non_public_schema"         |   "posts"     |   "con1"
            null                        |   "users"     |   "this_is_constraint"
            "public"                    |   "users"     |   "this_is_constraint"
            "non_public_schema"         |   "users"     |   "this_is_constraint"
    }

    def cleanup() {
        if (testConstraint != null)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ALTER TABLE ")
            if (testSchema != null)
            {
                stringBuilder.append(testSchema)
                stringBuilder.append(".")
            }
            stringBuilder.append(testTable)
            stringBuilder.append(" DROP CONSTRAINT IF EXISTS ")
            stringBuilder.append(testConstraint)
            stringBuilder.append(";")
            jdbcTemplate.execute(stringBuilder.toString())
            assertEquals(false, isAnyRecordExists(jdbcTemplate, createSelectStatement(testSchema, testTable, testConstraint)))
        }
    }

    String createSelectStatement(String schema, String table, String constraintName)
    {
        def template = "SELECT 1\n" +
                "\t\tFROM information_schema.table_constraints\n" +
                "\t\tWHERE table_schema = '%s' AND table_name = '%s' AND constraint_name = '%s'"
        String.format(template, schema == null ? "public" : schema, table, constraintName)
    }

    protected abstract P returnTestedObject()

    protected abstract X returnCorrectParametersMockObject()
}
