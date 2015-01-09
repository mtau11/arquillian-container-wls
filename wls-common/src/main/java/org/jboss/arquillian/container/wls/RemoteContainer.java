/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.container.wls;

import java.io.File;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;

/**
 * A utility class for performing operations relevant to a remote WebLogic container used by Arquillian.
 * 
 * <p>
 * This uses a combination of the Deployer utility and JMX. The Deployer utility is used for the actual deployment
 * and undeployment, while JMX is used for verification.
 * 
 * <p>
 * This implementation is not 100% stable and is known to fail occasionally. 
 * 
 * @author Vineet Reynolds
 *
 */
public class RemoteContainer {

    private WebLogicJMXClient jmxClient;
    private WebLogicDeployerClient deployerClient;
    private CommonWebLogicConfiguration configuration;

    public RemoteContainer(CommonWebLogicConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Starts a JMX client to read container metadata from the Domain Runtime MBean Server.
     * 
     * @throws LifecycleException When a connection cannot be created to the MBean Server.
     */
    public void start() throws LifecycleException {
        deployerClient = new WebLogicDeployerClient(configuration);
        jmxClient = new WebLogicJMXClient(configuration);
    }

    /**
     * Wraps the operation of forking a weblogic.Deployer process to deploy an application.
     * 
     * @param archive The ShrinkWrap archive to deploy
     * @return The metadata for the deployed application
     * @throws DeploymentException When forking of weblogic.Deployer fails, or when interaction with the forked process fails,
     *         or when details of the deployment cannot be obtained from the Domain Runtime MBean Server.
     */
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String deploymentName = getDeploymentName(archive);
        File deploymentArchive = ShrinkWrapUtil.toFile(archive, configuration.isDeployExplodedArchive());

        deployerClient.deploy(deploymentName, deploymentArchive);
        return jmxClient.verifyDeployment(deploymentName);
    }

    /**
     * Wraps the operation of forking a weblogic.Deployer process to undeploy an application.
     * 
     * @param archive The ShrinkWrap archive to undeploy
     * @throws DeploymentException When forking of weblogic.Deployer fails, or when interaction with the forked process fails,
     *         or when undeployment cannot be confirmed.
     */
    public void undeploy(Archive<?> archive) throws DeploymentException {
        // Undeploy the application
        String deploymentName = getDeploymentName(archive);
        deployerClient.undeploy(deploymentName);

        // Verify the undeployment from the Domain Runtime MBean Server.
        jmxClient.verifyUndeployment(deploymentName);
    }
    
    /**
     * Stops the JMX client.
     * 
     * @throws LifecycleException When there is failure in closing the JMX connection.
     */
    public void stop() throws LifecycleException {
        jmxClient.close();
    }

    private String getDeploymentName(Archive<?> archive) {
        String archiveFilename = archive.getName();
        int indexOfDot = archiveFilename.indexOf(".");
        if (indexOfDot != -1) {
            return archiveFilename.substring(0, indexOfDot);
        }
        return archiveFilename;
    }

}
