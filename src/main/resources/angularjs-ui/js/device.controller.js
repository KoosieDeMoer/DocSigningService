'use strict';
var module = angular.module("MainModule", []);

module
		.controller(
				"MainController",
				function($scope, $http, $window) {

					$scope.mode = null;
					$scope.contractBuilderService = null;
					$scope.details = null;
					$scope.captureAmount = false;
					$scope.acceptButtonText;
					$scope.approved = null;
					$scope.ref = "";
					$scope.who = (new window.URL(location.href)).searchParams
							.get('who');

					document.querySelector('div#who').style = 'color: white; text-align: center; font-size: 48px; height: 50px; width: 480px; background-color: ' + (($scope.who == 'Alice') ? 'green' : 'red');
					document.querySelector('div#who').innerHTML = $scope.who;

					$scope.authorisationPrompt = 'Do you approve?';

					_refreshPage();

					$scope.authorise = function() {
						$scope.respond('Yes');
					};

					$scope.decline = function() {
						$scope.respond('No');
					};

					$scope.respond = function(option) {
						console.log('Responding: ' + option);
						document.body.style.cursor = 'wait';
						$http(
								{
									method : 'POST',
									url : '/DocSigner/authResponse?responseOption='
											+ option
								}).then(function successCallback(response) {
							document.body.style.cursor = 'default';
						}, _error);
					};

					$scope.newDocForm = {
						emCertId : "",
						name : "",
						registered : false
					};
					/* Private Methods */

					function _refreshPage() {
						_registerEventSourceListener();

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
												$scope.authorisationPrompt = res[2];
												document
														.getElementById('authorisationPromptDiv').innerHTML = $scope.authorisationPrompt;
												angular.element('#authPrompt')
														.modal('show');
											} else if (res[1] == 'receivedoc') {
												// ignore - dealt with by
												// desktop
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

					function _success(response) {
						$scope.errorMessage = false;
						document.body.style.cursor = 'default';
					}

					function _error(response) {
						$scope.errorMessage = response.data;
						document.body.style.cursor = 'default';
					}
					;
				});
