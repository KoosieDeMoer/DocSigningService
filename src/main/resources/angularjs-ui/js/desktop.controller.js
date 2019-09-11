'use strict';
var module = angular.module("MainModule", []);

const SIGN_MODE = 'sign';
const SEND_MODE = 'send';
const VIEW_MODE = 'view';

module.directive('fileModel', [ '$parse', function($parse) {
	return {
		restrict : 'A',
		link : function(scope, element, attrs) {
			var model = $parse(attrs.fileModel);
			var modelSetter = model.assign;

			element.bind('change', function() {
				scope.$apply(function() {
					modelSetter(scope, element[0].files[0]);
				});
			});
		}
	};
} ]);

module.service('fileUpload', [ '$http', function($http) {
} ]);

module
		.controller(
				"MainController",
				function($scope, $http, $window) {

					$scope.mode = null;

					$scope.model = {
						name : "test_doc.pdf",
						brand : "unknown",
						docUrl : ""
					};

					$scope.signButtonDisabled = false;
					
					$scope.who = (new window.URL(location.href)).searchParams.get('who');
					
					document.querySelector('div#who').style = 'color: white; text-align: center; font-size: 48px; height: 50px; width: 378px; background-color: ' + (($scope.who == 'Alice') ? 'green' : 'red');
					document.querySelector('div#who').innerHTML = $scope.who;
					
					
					
					$scope.loadDoc = function() {

						$scope.mode = SIGN_MODE;
						
						if ($scope.model.name == 'cheque') {
							$scope.isCheque = true;
						} else {
							document.body.style.cursor = 'wait';
							$http(
									{
										method : 'GET',
										url : '/DocSigner/numberAndMarkDoc/'
												+ $scope.model.name
									})
									.then(
											function successCallback(response) {
												document.body.style.cursor = 'default';
												$scope.model.docUrl = "/ViewerJS/#/DocSigner/docs/marked/"
														+ $scope.model.name;
												$scope.docLoaded = true;
											}, _error);

						}

					};

					$scope.loadCheque = function() {
						document.body.style.cursor = 'wait';

						$http({
							method : 'GET',
							url : '/DocSigner/prepCheque?payee='+ $scope.model.payee + '&amountCents=' + $scope.model.amountCents
						})
								.then(
										function successCallback(response) {
											$scope.model.name = 'cheque.pdf';
											$scope.loadDoc();
											}, _error);

					};

					$scope.signDoc = function() {
						document.body.style.cursor = 'wait';
						$scope.signButtonDisabled = true;
						$http({
							method : 'GET',
							url : '/DocSigner/signDoc/' + $scope.model.name + '?who=' + $scope.who
						})
								.then(
										function successCallback(response) {
											console.log(response.data);
											if (response.data == 'DECLINED') {

											} else {
												$scope.model.docUrl = "/ViewerJS/#/DocSigner/docs/signed/"
														+ $scope.model.name;
												$scope.docSigned = true;
											}
											document.body.style.cursor = 'default';
										}, _error);

						$scope.signButtonDisabled = false;

					};

					$scope.closeDoc = function() {
						    $scope.docLoaded = false;
						    $scope.docSigned = false;
						    
					}

					$scope.uploadDoc = function() {
						$scope.mode = SEND_MODE;
						uploadSomething('/DocSigner/uploadFile', $scope.model.fileToSend, downloadDoc);
						$scope.model.name = $scope.model.fileToSend.name;

					}
					
					function downloadDoc() {
						// first apply the identicon
						$http(
								{
									method : 'GET',
									url : '/DocSigner/numberDoc/'
											+ $scope.model.name
								})
								.then(
										function successCallback(response) {
											document.body.style.cursor = 'default';
											$scope.model.docUrl = "/ViewerJS/#/DocSigner/docs/identified/"
													+ $scope.model.name;
											$scope.docLoaded = true;
										}, _error);

						
						// uploadFile
					}
					
					$scope.sendDoc = function() {
						document.body.style.cursor = 'wait';
						$scope.signButtonDisabled = true;
						$http({
							method : 'GET',
							url : '/DocSigner/sendDoc/' + $scope.model.name + '?from=' + $scope.who + '&to=' + $scope.model.recipient
						})
								.then(
										function successCallback(response) {
											console.log('response.data');
											console.log(response.data);
											if (response.data == 'DECLINED') {

											} else {
												var rowData = {document: $scope.model.name , link: "javascript:void(0);", recipient: $scope.model.recipient, status: 'sent'};
												$scope.sentItems.push(rowData);
											}
											$scope.docLoaded = false;
											document.body.style.cursor = 'default';
										}, _error);

						$scope.signButtonDisabled = false;


					}

					function uploadSomething(uploadUrl, file, callback) {
						var fd = new FormData();
						fd.append('file', file);
						$http
								.post(
										uploadUrl,
										fd,
										{
											transformRequest : angular.identity,
											headers : {
												'Content-Type' : undefined
											}
										})
										.success((response) => {if(callback !== undefined) {
											callback();
										}})
								.error(function() {
									_error(response);
								});
					}

					
					$scope.requestDoc = function(rowData) {
						if(rowData.status == 'available') {
						document.body.style.cursor = 'wait';
						$scope.signButtonDisabled = true;
						$http({
							method : 'GET',
							url : '/DocSigner/requestDoc/' + rowData.document + '?to=' + $scope.who + '&from=' + rowData.sender
						})
								.then(
										function successCallback(response) {
											console.log('response.data');
											console.log(response.data);
											if (response.data == 'DECLINED') {

											} else {
												var indexOfRowToChange = $scope.inbox.findIndex(object => object.document == rowData.document);
												console.log(indexOfRowToChange);
												var rowToChange = {document: rowData.document , link: rowData.link, sender: rowData.sender, status: 'read'};
												$scope.inbox[indexOfRowToChange] = rowToChange;
											}
											document.body.style.cursor = 'default';
										}, _error);
						} else if (rowData.status == 'read') {
							$scope.model.docUrl = "/ViewerJS/#/DocSigner/docs/recipient-added/"
									+ rowData.document;
							$scope.mode = VIEW_MODE;
							$scope.docLoaded = true;

						}
					
					}

					$scope.viewDoc = function(rowData) {
						if(rowData.status == 'read') {
							$scope.model.docUrl = "/ViewerJS/#/DocSigner/docs/recipient-added/"
								+ rowData.document;
							$scope.mode = VIEW_MODE;
						    $scope.docLoaded = true;
							
						} else if (rowData.status == 'sent') {
							$scope.model.docUrl = "/ViewerJS/#/DocSigner/docs/sender-added/"
									+ rowData.document;
							$scope.mode = VIEW_MODE;
							$scope.docLoaded = true;
						}					
					}

					
					
					function _registerEventSourceListener() {

						var feed = new EventSource('/event-source');
						var handler = function(event) {
							$scope
									.$apply(function() {
										console.log('from event source:'
												+ event.data);
										var res = event.data.split('|');
										if (res[0] == $scope.who) {

											if (res[1] == 'authoriserequest') {
												// do nothing - device deals
												// with
											}
											else if (res[1] == 'receivedoc') {
												// show the doc view or request
												// link
												if(res[2] == 'recipient') {
													var indexOfRowToChange = $scope.inbox.findIndex(object => object.document == res[3]);
													var rowData = {document: res[3] , link: res[5], sender: res[4], status: res[6]};
													if(indexOfRowToChange == -1) {
														$scope.inbox.push(rowData);
													} else {
														$scope.inbox[indexOfRowToChange] = rowData;
													}
												} else if(res[2] == 'sender'){ // sender
													var indexOfRowToChange = $scope.sentItems.findIndex(object => object.document == res[3]);
													var rowData = {document: res[3] , link: res[5], recipient: res[4], status: res[6]};
													if(indexOfRowToChange == -1) {
														$scope.sentItems.push(rowData);
													} else {
														$scope.sentItems[indexOfRowToChange] = rowData;
													}
												} else {
													console
													.log('Unexpected party: '
															+ event.data);
													
												}
												
											} else {
												console
														.log('Unexpected event: '
																+ event.data);
											}
										}
									});

						}
						feed.addEventListener('message', handler, false);
					}

					
					function _getBrand() {
						document.body.style.cursor = 'wait';
						$scope.signButtonDisabled = true;
						$http({
							method : 'GET',
							url : '/Other/brand'
						})
								.then(
										function successCallback(response) {
											$scope.model.brand = response.data;
											document.body.style.cursor = 'default';
										}, _error);

					}

					$scope.sentItems = [];
					$scope.inbox = [];
					
				    _registerEventSourceListener();
				    _getBrand();
				    
				    
				    
					function _success(response) {
						$scope.errorMessage = false;
						$scope.connected = true;
						document.body.style.cursor = 'default';
					}

					function _error(response) {
						$scope.errorMessage = response.data;
						document.body.style.cursor = 'default';
					}

					$scope.showPopover = function() {
						$scope.popoverIsVisible = true;
					};

					$scope.hidePopover = function() {
						$scope.popoverIsVisible = false;
					};

					;
				});
