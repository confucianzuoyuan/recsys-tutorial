import { Component, OnInit } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Product} from "../model/product";
import {LoginService} from "../services/login.service";
import {Router} from "@angular/router";
import {constant} from "../model/constant";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  streamProducts: Product[] = [];
  hotProducts: Product[] = [];
  rateMoreProducts: Product[] = [];
  offlineProducts: Product[] = [];

  constructor(private httpService : HttpClient,private loginService:LoginService, private router:Router) {}

  ngOnInit(): void {
    this.getStreamProducts();
    this.getHotProducts();
    this.getRateMoreProducts();
    this.getOfflineProducts();
  }

  getStreamProducts(): void {
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/stream?num=6&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.streamProducts = data['products'].slice(0, 6);
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getHotProducts(): void {
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/hot?num=6&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.hotProducts = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getRateMoreProducts(): void {
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/rate?num=6&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.rateMoreProducts = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getOfflineProducts(): void {
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/offline?num=6&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.offlineProducts = data['products'].slice(0, 6);
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }
}
