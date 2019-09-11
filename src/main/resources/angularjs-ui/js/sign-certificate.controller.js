'use strict';
var module = angular.module("MainModule", []);

module.controller("MainController", function($scope, $http, $window) {

	_refreshPage();

	$scope.signCertificate = function() {
		document.body.style.cursor = 'wait';
		$http(
				{
					method : 'PUT',
					transformResponse: undefined, 
					url : '/DocSigner/signCertificate',
					data : 	$scope.newDocForm.csr
				}).then(function successCallback(response) {

			console.log(response.data);

			$scope.newDocForm.certInfo = response.data;

			$scope.newDocForm.signed = true;

			document.body.style.cursor = 'default';

		}, function errorCallback(response) {
			_error(response);
		});

	};

	$scope.newDocForm = {
		name : "",
		organisation : "",
		country : "",
		signed : false,
		csr : "",
		certInfo : ""
	};
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
});
