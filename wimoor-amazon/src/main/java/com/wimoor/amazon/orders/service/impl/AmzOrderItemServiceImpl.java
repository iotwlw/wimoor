package com.wimoor.amazon.orders.service.impl;

import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazon.spapi.api.OrdersV0Api;
import com.amazon.spapi.client.ApiCallback;
import com.amazon.spapi.client.ApiException;
import com.amazon.spapi.model.orders.GetOrderItemsResponse;
import com.amazon.spapi.model.orders.OrderItem;
import com.amazon.spapi.model.orders.OrderItemList;
import com.amazon.spapi.model.orders.OrderItemsList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.pojo.entity.Marketplace;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.auth.service.IAmzAuthApiTimelimitService;
import com.wimoor.amazon.auth.service.IMarketplaceService;
import com.wimoor.amazon.auth.service.impl.ApiBuildService;
import com.wimoor.amazon.orders.mapper.AmzOrderItemMapper;
import com.wimoor.amazon.orders.mapper.AmzOrderMainMapper;
import com.wimoor.amazon.orders.pojo.entity.AmzOrderItem;
import com.wimoor.amazon.orders.pojo.entity.AmzOrderMain;
import com.wimoor.amazon.orders.service.IAmzOrderItemService;
import com.wimoor.amazon.util.AmzUtil;
import com.wimoor.common.GeneralUtil;

import cn.hutool.core.util.StrUtil;
@Service("amzOrderItemService")
public class AmzOrderItemServiceImpl extends ServiceImpl<AmzOrderItemMapper,AmzOrderItem> implements IAmzOrderItemService {
	@Resource
	AmzOrderMainMapper amzOrderMainMapper;
	@Resource
	IAmazonAuthorityService amazonAuthorityService;
	@Resource
	IAmzAuthApiTimelimitService iAmzAuthApiTimelimitService;
	@Resource
	IMarketplaceService marketplaceService;
	@Autowired
	ApiBuildService apiBuildService;
	
 
	@Override
	public void ordersItem(AmzOrderMain order,String token) {
		// TODO Auto-generated method stub
		   AmazonAuthority auth = amazonAuthorityService.getById(order.getAmazonauthid());
		   if(auth!=null) {
	          OrdersV0Api api = apiBuildService.getOrdersV0Api(auth);
				 if(api!=null) {
						ApiCallback<GetOrderItemsResponse> apiCallbackGetOrderItems=new ApiCallbackGetOrderItems(this,auth,order);
						try {
							if(auth.apiNotRateLimit("getOrderItems")) {
								api.getOrderItemsAsync(order.getAmazonOrderId(),token,apiCallbackGetOrderItems);
							}
							
						 } catch (ApiException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
	               }
	       }

	}
	
	@Override
	public void handlerOrderItemListResponse(AmazonAuthority auth, AmzOrderMain order, OrderItemsList itemsList) {
		// TODO Auto-generated method stub
		if(itemsList!=null) {
			OrderItemList items = itemsList.getOrderItems();
			String orderid = itemsList.getAmazonOrderId();
			if(items!=null) {
				for(OrderItem orderitem:items) {
					AmzOrderItem item=new AmzOrderItem();
					item.setPurchaseDate(order.getPurchaseDate());
					item.setAmazonauthid(auth.getId());
					item.setAsin(orderitem.getASIN());
					item.setSku(orderitem.getSellerSKU());
					item.setAmazonOrderId(orderid);
					//item.setTitle(orderitem.getTitle());
					item.setCodfee(AmzUtil.getMoneny(orderitem.getCoDFee()));
					item.setCodfeediscount(AmzUtil.getMoneny(orderitem.getCoDFeeDiscount()));
					item.setConditionid(orderitem.getConditionId());
					item.setConditionsubtypeid(orderitem.getConditionSubtypeId());
					item.setConditionnote(orderitem.getConditionNote());
					item.setItemPrice(AmzUtil.getMoneny(orderitem.getItemPrice()));
					item.setItemTax(AmzUtil.getMoneny(orderitem.getItemTax()));
					item.setItemPromotionDiscount(AmzUtil.getMoneny(orderitem.getPromotionDiscount()));
					item.setShipPromotionDiscount(AmzUtil.getMoneny(orderitem.getShippingDiscount()));
					item.setOrderitemid(orderitem.getOrderItemId());
					item.setQuantityordered(orderitem.getQuantityOrdered());
					item.setQuantityshipped(orderitem.getQuantityShipped());
					item.setOrderitemid(orderitem.getOrderItemId());
					item.setScheduleddeliverystartdate(GeneralUtil.getDatez(orderitem.getScheduledDeliveryStartDate()));
					item.setScheduleddeliveryenddate(GeneralUtil.getDatez(orderitem.getScheduledDeliveryEndDate()));
			        item.setPromotionIds(AmzUtil.getIds(orderitem.getPromotionIds()));
					item.setMarketplaceid(order.getMarketplaceid());
					item.setShippingTax(AmzUtil.getMoneny(orderitem.getShippingTax()));
					item.setShippingPrice(AmzUtil.getMoneny(orderitem.getShippingPrice()));
					QueryWrapper<AmzOrderItem> query=new QueryWrapper<AmzOrderItem>();
					query.eq("amazon_order_id",item.getAmazonOrderId());
					query.eq("orderItemId", item.getOrderitemid());
					AmzOrderItem oldone = this.baseMapper.selectOne(query);
					if(oldone!=null) {
						this.baseMapper.update(item, query);
					}else {
						this.baseMapper.insert(item);
					}
				}
			}
			if(StrUtil.isBlank(itemsList.getNextToken())) {
				order.setHasItem(true);
				amzOrderMainMapper.updateById(order);
			}else {
				ordersItem(order,itemsList.getNextToken());
			}
		}
	}

	@Override
	public void runApi(AmazonAuthority amazonAuthority) {
		// TODO Auto-generated method stub
		 List<Marketplace> marketlist = marketplaceService.selectMarketBySellerid(amazonAuthority.getSellerid(),amazonAuthority.getShopId());
		   //遍历auth下的marketplaceid
		   if(marketlist!=null && marketlist.size()>0) {
			   for(int j=0;j<marketlist.size();j++) {
				   Marketplace marketplace = marketlist.get(j);
				   QueryWrapper<AmzOrderMain> query=new QueryWrapper<AmzOrderMain>();
				   query.eq("amazonauthid", amazonAuthority.getId());
				   Calendar c=Calendar.getInstance();
				   c.add(Calendar.HOUR, -8);
				   GeneralUtil.getDatePlus(c, marketplace.getMarket());
				   c.add(Calendar.MINUTE, -10);
				   Calendar cstart=Calendar.getInstance();
				   cstart.setTime(c.getTime());
				   cstart.add(Calendar.HOUR, 24);
				   query.gt("purchase_date", cstart.getTime());
				   query.lt("purchase_date", c.getTime());
				   query.eq("hasItem", false);
				   query.orderByAsc("purchase_date");
				   List<AmzOrderMain> orders = amzOrderMainMapper.selectList(query);
				   for(AmzOrderMain order:orders) {
					  ordersItem(order,null);
				   }
			   }
		   }
	}
}
