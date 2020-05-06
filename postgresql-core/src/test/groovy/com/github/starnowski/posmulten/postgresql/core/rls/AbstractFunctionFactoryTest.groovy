package com.github.starnowski.posmulten.postgresql.core.rls

import spock.lang.Specification
import spock.lang.Unroll

abstract class AbstractFunctionFactoryTest extends Specification {

    def "should return non-empty string object for correct parameters object"() {
        given:
            AbstractFunctionFactory tested = returnTestedObject()
            IFunctionFactoryParameters parameters = returnCorrectParametersSpyObject()

        when:
            String result = tested.produce(parameters)

        then:
            result != null
            !result.trim().isEmpty()
    }

    def "should throw exception of type 'IllegalArgumentException' when parameters object is null" ()
    {
        given:
            AbstractFunctionFactory tested = returnTestedObject()

        when:
            tested.produce(null)

        then:
            def ex = thrown(IllegalArgumentException.class)

        and: "exception should have correct message"
            ex.message == "The parameters object cannot be null"
    }

    def "should throw exception of type 'IllegalArgumentException' when function name is null, even if the rest of parameters are correct"()
    {
        given:
            AbstractFunctionFactory tested = returnTestedObject()
            IFunctionFactoryParameters parameters = returnCorrectParametersSpyObject()

        when:
            String result = tested.produce(parameters)

        then:
            1 * parameters.getFunctionName() >> null
            def ex = thrown(IllegalArgumentException.class)
    }

    @Unroll
    def "should throw exception of type 'IllegalArgumentException' when function name is blank ('#functionName'), even if the rest of parameters are correct"()
    {
        given:
            AbstractFunctionFactory tested = returnTestedObject()
            IFunctionFactoryParameters parameters = returnCorrectParametersSpyObject()

        when:
            String result = tested.produce(parameters)

        then:
            (1.._) * parameters.getFunctionName() >> functionName
            def ex = thrown(IllegalArgumentException.class)

        where:
            functionName << ["", "  ", "            "]
    }

    abstract protected returnTestedObject();

    abstract protected returnCorrectParametersSpyObject();
}
