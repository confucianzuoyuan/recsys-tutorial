import { Component, OnInit } from '@angular/core';
import {Product} from "../model/product";
import {HttpClient} from "@angular/common/http";
import {ActivatedRoute} from "@angular/router";
import 'rxjs/add/operator/switchMap';
import {constant} from "../model/constant";

@Component({
  selector: 'app-mdetail',
  templateUrl: './mdetail.component.html',
  styleUrls: ['./mdetail.component.css']
})
export class MdetailComponent implements OnInit {

  product: Product = new Product;
  sameProducts: Product[] = [];
  cbProducts: Product[] = [];

  constructor(
    private route: ActivatedRoute,
    private httpService : HttpClient,
  ) {}

  ngOnInit(): void {
    this.route.params
      .subscribe(params => {
        var id = params['id'];
        this.getProductInfo(id);
        this.getSameProducts(id);
        this.getCbProducts(id);
    });
  }

  getSameProducts(id:number):void{
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'/rest/product/itemcf/' + id)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.sameProducts = data['products']
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }

  getCbProducts(id: number): void {
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL + '/rest/product/contentbased/' + id)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.cbProducts = data['products'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      )
  }

  getProductInfo(id:number):void{
    this.httpService
      .get(constant.BUSSINESS_SERVER_URL+'/rest/product/info/'+id)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.product = data['product'];
          }
        },
        err => {
          console.log('Something went wrong!');
        }
      );
  }
}
