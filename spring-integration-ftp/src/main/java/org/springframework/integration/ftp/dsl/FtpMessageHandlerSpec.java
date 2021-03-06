/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ftp.dsl;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.dsl.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;

/**
 * A {@link FileTransferringMessageHandlerSpec} for FTP.
 *
 * @author Artem Bilan
 * @author Joaquin Santana
 *
 * @since 5.0
 */
public class FtpMessageHandlerSpec extends FileTransferringMessageHandlerSpec<FTPFile, FtpMessageHandlerSpec> {

	FtpMessageHandlerSpec(SessionFactory<FTPFile> sessionFactory) {
		this.target = new FtpMessageHandler(sessionFactory);
	}

	FtpMessageHandlerSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate) {
		this.target = new FtpMessageHandler(remoteFileTemplate.getSessionFactory());
	}

	FtpMessageHandlerSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate, FileExistsMode fileExistsMode) {
		this.target = new FtpMessageHandler(remoteFileTemplate, fileExistsMode);
	}

}
