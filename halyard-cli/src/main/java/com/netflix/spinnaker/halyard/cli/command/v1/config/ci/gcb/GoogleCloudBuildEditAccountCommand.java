/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.halyard.cli.command.v1.config.ci.gcb;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.netflix.spinnaker.halyard.cli.command.v1.NestableCommand;
import com.netflix.spinnaker.halyard.cli.command.v1.config.ci.master.AbstractHasAccountCommand;
import com.netflix.spinnaker.halyard.cli.services.v1.Daemon;
import com.netflix.spinnaker.halyard.cli.services.v1.OperationHandler;
import com.netflix.spinnaker.halyard.cli.ui.v1.AnsiUi;
import com.netflix.spinnaker.halyard.config.model.v1.ci.gcb.GoogleCloudBuildAccount;
import com.netflix.spinnaker.halyard.config.model.v1.node.CIAccount;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Parameters(separators = "=")
public class GoogleCloudBuildEditAccountCommand extends AbstractHasAccountCommand {
  @Getter(AccessLevel.PROTECTED)
  private Map<String, NestableCommand> subcommands = new HashMap<>();

  protected String getCiName() {
    return "gcb";
  }

  public String getShortDescription() {
    return "Add a Google Cloud Build account";
  }

  @Getter(AccessLevel.PUBLIC)
  private String commandName = "edit";

  @Parameter(
      names = "--project",
      description = "The name of the GCP in which to trigger and monitor builds"
  )
  private String project;

  @Parameter(
      names = "--subscriptionName",
      description = "The name of the PubSub subscription on which to listen for build changes"
  )
  public String subscriptionName;

  @Parameter(
      names = "--jsonKey",
      description = "The path to a JSON service account that Spinnaker will use as credentials"
  )
  public String jsonKey;

  protected GoogleCloudBuildAccount editAccount(GoogleCloudBuildAccount account) {
    if (isSet(project)) {
      account.setProject(project);
    }

    if (isSet(subscriptionName)) {
      account.setSubscriptionName(subscriptionName);
    }

    if (isSet(jsonKey)) {
      account.setJsonKey(jsonKey);
    }

    return account;
  }

  @Override
  protected void executeThis() {
    String accountName = getAccountName();
    String ciName = getCiName();
    String currentDeployment = getCurrentDeployment();
    // Disable validation here, since we don't want an illegal config to prevent us from fixing it.
    GoogleCloudBuildAccount account = (GoogleCloudBuildAccount) new OperationHandler<CIAccount>()
        .setOperation(Daemon.getMaster(currentDeployment, ciName, accountName, false))
        .setFailureMesssage(String.format("Failed to get Google Cloud Build Account %s.", accountName))
        .get();

    int originalHash = account.hashCode();

    account = editAccount(account);

    if (originalHash == account.hashCode()) {
      AnsiUi.failure("No changes supplied.");
      return;
    }

    new OperationHandler<Void>()
        .setOperation(Daemon.setMaster(currentDeployment, ciName, accountName, !noValidate, account))
        .setSuccessMessage(String.format("Edited Google Cloud Build account %s.", accountName))
        .setFailureMesssage(String.format("Failed to edit Google Cloud Build account %s.", accountName))
        .get();
  }
}
