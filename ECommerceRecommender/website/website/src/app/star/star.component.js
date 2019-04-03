"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
var core_1 = require("@angular/core");
var StarComponent = (function () {
    function StarComponent() {
        this.currentValue = 0;
        this.tempValue = null;
        this.rating = false;
        this.setRate = false;
        this.stars = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
    }
    StarComponent.prototype.ngOnInit = function () {
    };
    StarComponent.prototype.getState = function (index) {
        if (index < this.currentValue) {
            return (index % 2 == 0) ? "scoreBefore" : "scoreAfter";
        }
        return "";
    };
    StarComponent.prototype.isScoreBefore = function (index) {
        if (this.rating)
            return false;
        if (index < this.currentValue) {
            return (index % 2 == 0) ? true : false;
        }
        return false;
    };
    StarComponent.prototype.isScoreAfter = function (index) {
        if (this.rating)
            return false;
        if (index < this.currentValue) {
            return (index % 2 == 0) ? false : true;
        }
        return false;
    };
    StarComponent.prototype.isNoScoreBefore = function (index) {
        if (index < this.currentValue) {
            return false;
        }
        return (index % 2 == 0) ? true : false;
    };
    StarComponent.prototype.isNoScoreAfter = function (index) {
        if (index < this.currentValue) {
            return false;
        }
        return (index % 2 == 0) ? false : true;
    };
    StarComponent.prototype.isRatingBefore = function (index) {
        if (!this.rating)
            return false;
        if (index < this.currentValue) {
            return (index % 2 == 0) ? true : false;
        }
        return false;
    };
    StarComponent.prototype.isRatingAfter = function (index) {
        if (!this.rating)
            return false;
        if (index < this.currentValue) {
            return (index % 2 == 0) ? false : true;
        }
        return false;
    };
    StarComponent.prototype.hover = function (index) {
        this.rating = true;
        if (this.tempValue == null)
            this.tempValue = this.currentValue;
        this.currentValue = index + 1;
    };
    StarComponent.prototype.rate = function (index) {
        this.setRate = true;
        this.currentValue = index + 1;
        this.rating = true;
        console.log(this.currentValue);
    };
    StarComponent.prototype.leave = function () {
        if (!this.setRate) {
            this.rating = false;
            this.currentValue = this.tempValue;
            this.tempValue = null;
        }
    };
    __decorate([
        core_1.Input(),
        __metadata("design:type", Number)
    ], StarComponent.prototype, "currentValue", void 0);
    StarComponent = __decorate([
        core_1.Component({
            selector: 'app-star',
            templateUrl: './star.component.html',
            styleUrls: ['./star.component.css']
        }),
        __metadata("design:paramtypes", [])
    ], StarComponent);
    return StarComponent;
}());
exports.StarComponent = StarComponent;
//# sourceMappingURL=star.component.js.map