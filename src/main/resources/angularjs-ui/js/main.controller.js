'use strict';
var module = angular.module("MainModule", []);

module.controller("MainController", function($scope, $http, $window) {

	var feed; // eventsource
	
	_refreshPage();

	/* Private Methods */

	function _refreshPage() {

		document.body.style.cursor = 'wait';
		_registerEventSourceListener();
		
	}


	function _clearEventSourceListeners() {
		// first we clear all the EventSource emitters
		console.log('_clearEventSourceListeners');
		$http({
			method : 'GET',
			url : '/DocSigner/clearEventSources'
		}).then(_registerEventSourceListener(), function errorCallback(response) {
			_error(response);
		});
	}
	
	
	function _registerEventSourceListener() {
			
		
		console.log('_registerEventSourceListener');
		feed = new EventSource('/event-source');
		var handler = function(event) {
			$scope.$apply(function() {
				console.log('from event source:' + event.data);
				if (event.data.startsWith('activity_')) {
					indicatePending(event.data.substring(9));
				} else {
					// simply refresh when a signal arrives
					_refreshDemographic();
					// would be nice
					// document.getElementById(event.data).click();
					if (event.data == 'complete') {
						feed.close();
						document.body.style.cursor = 'default';
					}
				}
			});

		}
		feed.addEventListener('message', handler, false);
		setTimeout(_startDemo(), 2000);
		
	}

	function _startDemo() {

		console.log('_startDemo');

		$http({
			method : 'GET',
			url : '/Other/startDemo'
		}).then(function successCallback(response) {
			console.log('startDemo returned success');
		}, function errorCallback(response) {
			_error(response);
		});


	}

	function _refreshDemographic() {
		$scope.demos = null;
		$http({
			method : 'GET',
			url : '/Other/demographic'
		}).then(function successCallback(response) {
			// console.log('from demographic call:' + response.data);
			document.getElementById("svgDiv").innerHTML = response.data;
		}, function errorCallback(response) {
			_error(response);
		});

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
