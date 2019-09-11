'use strict';
var module = angular.module("MainModule", []);

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
				[
						'$scope',
						'$http',
						'$window',
						'fileUpload',
						function($scope, $http, $window) {

							_refreshPage();

							$scope.newDocForm = {
								name : "",
								organisation : "",
								country : "",
								csrGenerated : false,
								registered : false,
								signatureSrc : '/DocSigner/docs/signatures/lara croft.png',
								initialsSrc : '/DocSigner/docs/initials/lara croft.png',
								csr : "",
								certificate : ""
							};

							$scope.registerSigner = function() {
								console.log($scope.newDocForm.signatureFile);

								document.body.style.cursor = 'wait';
								$http(
										{
											method : 'GET',
											transformResponse : undefined,
											url : '/DocSigner/createSigner?alias='
													+ $scope.newDocForm.name
													+ '&O='
													+ $scope.newDocForm.organisation
													+ '&C='
													+ $scope.newDocForm.country
										})
										.then(
												function successCallback(
														response) {

													$scope.newDocForm.csr = response.data;

													if ($scope.newDocForm.signatureFile) {
														uploadSomething('/DocSigner/uploadSignature', $scope.newDocForm.signatureFile);
													}

													if ($scope.newDocForm.initialsFile) {
														uploadSomething('/DocSigner/uploadInitials', $scope.newDocForm.initialsFile);
													}

													$scope.newDocForm.csrGenerated = true;

													document.body.style.cursor = 'default';

												},
												function errorCallback(response) {
													_error(response);
												});

							};

							$scope.registerCertificate = function() {
								document.body.style.cursor = 'wait';
								$http(
										{
											method : 'PUT',
											transformResponse : undefined,
											url : '/DocSigner/registerCertificate?alias='
													+ $scope.newDocForm.name,
											data : $scope.newDocForm.certificate
										})
										.then(
												function successCallback(
														response) {

													
													$scope.newDocForm.signatureSrc = '/DocSigner/docs/signatures/'
															+ $scope.newDocForm.name
															+ '.png';
													$scope.newDocForm.initialsSrc = '/DocSigner/docs/initials/'
															+ $scope.newDocForm.name
															+ '.png';
													;

													$scope.newDocForm.registered = true;

													document.body.style.cursor = 'default';

												},
												function errorCallback(response) {
													_error(response);
												});

							};

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

							/* Private Methods */

							function _refreshPage() {
								// empty for now

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
						} ]);
