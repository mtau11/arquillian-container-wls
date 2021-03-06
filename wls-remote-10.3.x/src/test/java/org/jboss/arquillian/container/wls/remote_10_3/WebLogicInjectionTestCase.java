/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.wls.remote_10_3;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TestCase to verify support for @Resource and @EJB annotation based injection.
 *
 * @author Vineet Reynolds
 */
@RunWith(Arquillian.class)
public class WebLogicInjectionTestCase {

    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(WebLogicInjectionTestCase.class.getName());

    @Resource(name = "resourceInjectionTestName")
    private String injectedResource;

    @Resource(mappedName = "java:comp/UserTransaction")
    private UserTransaction transaction;

    @EJB(mappedName = "java:comp/env/ejb/Greeter")
    private Greeter greeter;

    /**
     * Deployment for the test
     */
    @Deployment
    public static Archive<?> getTestArchive() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
            .addClasses(GreeterServlet.class, WebLogicInjectionTestCase.class)
            .setWebXML("in-container-web-eartest.xml");
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "test.jar")
            .addClasses(Greeter.class, GreeterBean.class, GreeterRemote.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
            .setApplicationXML("application.xml")
            .addAsModule(ejb)
            .addAsModule(war);
        log.info(ear.toString(true));
        return ear;
    }

    @Test
    public void shouldBeAbleToInjectEjb() throws Exception {
        assertThat(injectedResource, equalTo("Hello World from an env-entry"));
        assertThat(transaction, notNullValue());
        assertThat(greeter, notNullValue());
        assertThat(greeter.greet(), equalTo("Hello"));
    }
}
