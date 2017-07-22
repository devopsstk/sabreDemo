package com.stk.demo;

import org.json.JSONException;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.munit.assertion.MunitAssertion;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonAssertion implements MunitAssertion {
	
	@Override
	public MuleEvent execute(MuleEvent muleEvent) throws AssertionError {
		
		String expectedResponse = muleEvent.getMessage().getProperty("expectedResult", PropertyScope.OUTBOUND);
		

//		String expectedResponse;
//		try {
//			expectedResponse = IOUtils.toString(
//				      this.getClass().getResourceAsStream("log4j2-test.xml"),
//				      "UTF-8"
//				    );
//		} catch (IOException e) {
//			throw new RuntimeException("could not read response file", e);
//		}
//
		try {
			JSONAssert.assertEquals(expectedResponse, muleEvent.getMessage().getPayload().toString(), false);
		} catch (JSONException e) {
			throw new AssertionError("unable to parse json", e);
		}

		return muleEvent;

	}
}
