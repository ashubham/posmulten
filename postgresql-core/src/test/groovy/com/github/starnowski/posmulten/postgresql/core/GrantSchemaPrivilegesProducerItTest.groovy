package com.github.starnowski.posmulten.postgresql.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.starnowski.posmulten.postgresql.core.TestUtils.selectAndReturnFirstRecordAsString
import static org.junit.Assert.assertEquals

@SpringBootTest(classes = [TestApplication.class])
class GrantSchemaPrivilegesProducerItTest extends Specification {

    @Autowired
    JdbcTemplate jdbcTemplate

    def tested = new GrantSchemaPrivilegesProducer()

    String user
    String schema
    String[] privileges
    String[] expectedPrivileges

    @Unroll
    def "should add \"#testPrivileges\" privileges to \"#testUser\" user for \"#testSchema\" schema" () {
        given:
            user = testUser
            schema = testSchema
            privileges = testPrivileges
            expectedPrivileges = testExpectedPrivileges
            for (String privilege : testExpectedPrivileges) {
                assertEquals("f", selectAndReturnFirstRecordAsString(jdbcTemplate, selectStatement(user, schema, privilege)))
            }

        when:
            jdbcTemplate.execute(tested.produce(testSchema, testUser, testPrivileges))

        then:
            for (String privilege : testExpectedPrivileges) {
                assertEquals("t", selectAndReturnFirstRecordAsString(jdbcTemplate, selectStatement(user, schema, privilege)))
            }

        where:
            testUser                        |   testSchema      |   testPrivileges          ||  testExpectedPrivileges
            "postgresql-core-user"          |   "public"        |   ["USAGE"]               ||  ["USAGE"]
            "postgresql-core-user"          |   "public"        |   ["CREATE"]              ||  ["CREATE"]
            "postgresql-core-user"          |   "public"        |   ["CREATE", "USAGE"]     ||  ["CREATE", "USAGE"]
            "postgresql-core-user"          |   "public"        |   ["ALL"]                 ||  ["CREATE", "USAGE"]
            "postgresql-core-user"          |   "public"        |   ["ALL PRIVILEGES"]      ||  ["CREATE", "USAGE"]
    }

    def cleanup() {
        for (String privilege : expectedPrivileges) {
            jdbcTemplate.execute("REVOKE " + privilege + " ON SCHEMA " + schema + " FROM  \"" + user + "\";")
            assertEquals("f", selectAndReturnFirstRecordAsString(jdbcTemplate, selectStatement(user, schema, privilege)))
        }
    }

    def selectStatement(String user, String schema, String privilege)
    {
        StringBuilder sb = new StringBuilder()
        sb.append("SELECT pg_catalog.has_schema_privilege('")
        sb.append(user)
        sb.append("', '")
        sb.append(schema)
        sb.append("', '")
        sb.append(privilege)
        sb.append("') AS USER_HAS_PRIVILEGE;")
        sb.toString()
    }
}
