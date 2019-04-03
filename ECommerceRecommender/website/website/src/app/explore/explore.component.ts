import { Component, OnInit } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Product} from "../model/product";
import {HttpClient} from "@angular/common/http";
import {LoginService} from "../services/login.service";
import {constant} from "../model/constant";

@Component({
  selector: 'app-explore',
  templateUrl: './explore.component.html',
  styleUrls: ['./explore.component.css']
})
export class ExploreComponent implements OnInit {

  constructor(public loginService:LoginService,public router:ActivatedRoute, public httpService:HttpClient) { }

  title: String;

  products: Product[] = [];

  ngOnInit() {
    this.router.params.subscribe(params => {
      if(params['type']=="search"){
        this.title = "搜索结果：";
        if(params['query'].trim() != "")
          this.getSearchProducts(params['query']);
      }else if(params['type']== "guess"){
        this.title = "猜你喜欢：";
        this.getGuessProducts();
      }else if(params['type']== "hot"){
        this.title = "热门推荐：";
        this.getHotProducts();
      }else if(params['type']== "rate"){
        this.title = "评分最多：";
        this.getRateMoreProducts();
      }else if(params['type']== "wish"){
        this.title = "我喜欢的：";
        this.getWishProducts();
      }
    });
  }

  getSearchProducts(query:String):void{
    console.log(query)
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/search?query='+query)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.products = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getGuessProducts():void{
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/guess?num=100&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.products = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getHotProducts():void{
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/hot?num=100&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.products = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getRateMoreProducts():void{
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/rate?num=100&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.products = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }
  getWishProducts():void{
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'rest/product/wish?num=100&username='+this.loginService.user.username)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.products = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

}
