package org.csu.mypetstore.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.user.domain.*;
import org.csu.mypetstore.user.domain.Views.*;
import org.csu.mypetstore.user.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {
    private static final Set<String>STATUSES=Set.of("PENDING_PAYMENT","PAID","SHIPPED","COMPLETED","CANCELLED");
    private final CartItemMapper carts;private final ProductMapper products;private final SalesOrderMapper orders;private final SalesOrderItemMapper items;
    public OrderService(CartItemMapper c,ProductMapper p,SalesOrderMapper o,SalesOrderItemMapper i){carts=c;products=p;orders=o;items=i;}
    public CheckoutToken snapshot(long userId){List<CartLine> lines=carts.lines(userId);validateLines(lines);return new CheckoutToken(lines.stream().map(CartLine::id).toList(),lines.stream().map(CartLine::version).toList(),total(lines));}
    public List<CartLine> cart(long userId){return carts.lines(userId);} public BigDecimal total(List<CartLine> l){return l.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
    @Transactional public long create(long userId,CheckoutToken token,String name,String phone,String address){
        name=required(name,50,"收货人");phone=required(phone,30,"联系电话");address=required(address,255,"详细地址");if(token==null)throw new StoreException("结算确认已失效，请重新确认");
        List<CartLine> lines=carts.lines(userId);validateLines(lines);if(!token.itemIds().equals(lines.stream().map(CartLine::id).toList())||!token.versions().equals(lines.stream().map(CartLine::version).toList())||token.total().compareTo(total(lines))!=0)throw new StoreException("购物车或价格已变化，请重新确认");
        for(CartLine line:lines)if(products.reserve(line.productId(),line.quantity())!=1)throw new StoreException(line.name()+" 库存不足或已下架");
        SalesOrder order=new SalesOrder();order.setOrderNo(orderNo());order.setUserId(userId);order.setStatus("PENDING_PAYMENT");order.setTotalAmount(total(lines));order.setReceiverName(name);order.setReceiverPhone(phone);order.setReceiverAddress(address);orders.insert(order);
        for(CartLine line:lines){SalesOrderItem item=new SalesOrderItem();item.setOrderId(order.getId());item.setProductId(line.productId());item.setSkuSnapshot(line.sku());item.setProductNameSnapshot(line.name());item.setCoverImageSnapshot(line.coverImage());item.setUnitPrice(line.price());item.setQuantity(line.quantity());item.setLineAmount(line.subtotal());items.insert(item);}
        if(carts.deleteChecked(userId,token.itemIds())!=lines.size())throw new StoreException("购物车已变化，下单未完成");return order.getId();
    }
    public IPage<OrderCard> list(long userId,String status,String orderNo,long page,long size){
        status=status==null||status.isBlank()?null:status;if(status!=null&&!STATUSES.contains(status))throw new StoreException("订单状态不正确");orderNo=orderNo==null?null:orderNo.strip();if(orderNo!=null&&orderNo.startsWith("MP-"))orderNo="DEMO-"+orderNo.substring(3);if(orderNo!=null&&orderNo.length()>40)throw new StoreException("订单号不能超过 40 个字符");if(page<1)page=1;if(size<1||size>50)throw new StoreException("分页参数不正确");
        IPage<SalesOrder> source=orders.owned(new Page<>(page,size),userId,status,escapeLike(orderNo));
        Page<OrderCard> result=new Page<>(source.getCurrent(),source.getSize(),source.getTotal());
        if(source.getRecords().isEmpty()){result.setRecords(List.of());return result;}
        Map<Long,List<OrderListLine>> lines=items.listLines(source.getRecords().stream().map(SalesOrder::getId).toList()).stream().collect(java.util.stream.Collectors.groupingBy(OrderListLine::orderId,LinkedHashMap::new,java.util.stream.Collectors.toList()));
        result.setRecords(source.getRecords().stream().map(order->orderCard(order,lines.getOrDefault(order.getId(),List.of()))).toList());return result;
    }
    public SalesOrder get(long userId,long id){SalesOrder o=orders.findOwned(id,userId);if(o==null)throw new StoreException("订单不存在");return o;}public List<OrderLine> lines(long orderId){return items.lines(orderId);}
    @Transactional public void cancel(long userId,long id){SalesOrder o=get(userId,id);if("CANCELLED".equals(o.getStatus()))return;if(!"PENDING_PAYMENT".equals(o.getStatus()))throw new StoreException("当前订单状态不能取消");if(orders.cancel(id,userId)!=1)throw new StoreException("订单状态已变化，请刷新后重试");for(OrderLine line:items.lines(id))products.restore(line.productId(),line.quantity());}
    @Transactional public void pay(long userId,long id,boolean success){SalesOrder o=get(userId,id);if(!success)throw new StoreException("模拟支付失败，订单仍可继续支付或取消");if("PAID".equals(o.getStatus()))return;if(!"PENDING_PAYMENT".equals(o.getStatus()))throw new StoreException("当前订单状态不能支付");String no="MOCK-"+o.getOrderNo()+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();if(orders.pay(id,userId,no)!=1)throw new StoreException("订单状态已变化，请刷新后重试");}
    @Transactional public void complete(long userId,long id){if(orders.complete(id,userId)!=1)throw new StoreException("只有已发货订单可以确认收货");}
    private static void validateLines(List<CartLine> lines){if(lines.isEmpty())throw new StoreException("购物车为空，不能结算");for(CartLine l:lines)if(!l.available())throw new StoreException(l.name()+" 已下架、售罄或库存不足，请先处理购物车");}
    private static OrderCard orderCard(SalesOrder order,List<OrderListLine> lines){
        int itemCount=lines.stream().mapToInt(line->line.quantity()==null?0:line.quantity()).sum();
        int imageLimit=itemCount>3?2:3;
        List<OrderThumb> thumbs=new ArrayList<>(imageLimit);
        for(OrderListLine line:lines){for(int i=0;i<(line.quantity()==null?0:line.quantity())&&thumbs.size()<imageLimit;i++)thumbs.add(new OrderThumb(line.productNameSnapshot(),line.coverImageSnapshot()));if(thumbs.size()==imageLimit)break;}
        int hiddenItemCount=Math.max(0,itemCount-thumbs.size());
        String names=lines.stream().map(OrderListLine::productNameSnapshot).filter(Objects::nonNull).distinct().limit(2).collect(java.util.stream.Collectors.joining("、"));
        String summary=names+(itemCount>lines.stream().map(OrderListLine::productNameSnapshot).filter(Objects::nonNull).distinct().limit(2).count()?" 等 ":" · 共 ")+itemCount+" 件";
        return new OrderCard(order.getId(),order.getOrderNo(),order.getStatus(),order.getTotalAmount(),order.getCreatedAt(),List.copyOf(thumbs),itemCount,hiddenItemCount,summary);
    }
    private static String escapeLike(String value){return value==null?null:value.replace("=","==").replace("%","=%").replace("_","=_");}
    private static String required(String v,int max,String label){if(v==null||v.strip().isEmpty())throw new StoreException(label+"不能为空");v=v.strip();if(v.length()>max)throw new StoreException(label+"长度不能超过 "+max+" 个字符");return v;}
    private static String orderNo(){return "MP"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))+String.format("%03d",new java.security.SecureRandom().nextInt(1000));}
}
