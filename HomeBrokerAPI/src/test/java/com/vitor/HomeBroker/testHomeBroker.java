package com.vitor.HomeBroker;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class testHomeBroker {

    @Test
    void contextLoads() {
    }
    @Test
    void testGetTransactionWithAlmostAllTotalOrders(){
        Ordem ordemTeste=new Ordem (50,100, LocalTime.of(14,30),3,"ATV001","compra","limitada",
                "456.735.487-90", "pendente");
        ArrayList<Ordem> totalOrdersTeste= new ArrayList<Ordem>();
        totalOrdersTeste.add(new Ordem (40,13,LocalTime.of(14,30),1,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (35,21,LocalTime.of(14,30),2,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (45,134,LocalTime.of(14,30),3,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (32,88,LocalTime.of(14,30),4,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (33,57,LocalTime.of(14,30),5,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (37,43,LocalTime.of(14,30),6,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (44,200,LocalTime.of(14,30),7,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (36,500,LocalTime.of(14,30),8,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (48,77,LocalTime.of(14,30),9,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (49,87,LocalTime.of(14,30),10,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (21,143,LocalTime.of(14,30),11,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (34,187,LocalTime.of(14,30),12,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (38,45,LocalTime.of(14,30),13,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (45,30,LocalTime.of(13,30),14,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (49,33,LocalTime.of(12,30),15,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (47,37,LocalTime.of(11,30),16,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        ArrayList<Ordem> resultList=    HomeBroker.getTransactionWithAlmostAllTotalOrders(ordemTeste.quantity,totalOrdersTeste,null);
        assertEquals(14,resultList.get(0).id);
        assertEquals(15,resultList.get(1).id);
        assertEquals(16,resultList.get(2).id);

        ordemTeste=new Ordem (50,100, LocalTime.of(14,30),3,"ATV001","compra","limitada",
                "456.735.487-90", "pendente");
        totalOrdersTeste= new ArrayList<Ordem>();
        totalOrdersTeste.add(new Ordem (40,13,LocalTime.of(14,30),1,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (35,22,LocalTime.of(14,30),2,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (45,130,LocalTime.of(14,30),3,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (32,88,LocalTime.of(14,30),4,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (33,57,LocalTime.of(14,30),5,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (37,44,LocalTime.of(14,30),6,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (44,200,LocalTime.of(14,30),7,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (36,500,LocalTime.of(14,30),8,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        ArrayList<Ordem> partialOrdersTeste=new ArrayList<>();
        partialOrdersTeste.add(new Ordem (33,1,LocalTime.of(14,30),10,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste.add(new Ordem (37,3,LocalTime.of(14,30),11,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste.add(new Ordem (44,6,LocalTime.of(14,30),12,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste.add(new Ordem (36,500,LocalTime.of(14,30),13,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));

        resultList=   HomeBroker.getTransactionWithAlmostAllTotalOrders(ordemTeste.quantity,totalOrdersTeste,partialOrdersTeste);
        assertEquals(1,resultList.get(0).id);
        assertEquals(2,resultList.get(1).id);
        assertEquals(5,resultList.get(2).id);
        assertEquals(10,resultList.get(3).id);
        assertEquals(11,resultList.get(4).id);
        assertEquals(12,resultList.get(5).id);
        assertEquals(6,resultList.size());
        assertEquals(4,resultList.get(5).quantity);
    }
    @Test
    void testGetTransactionWithMixedOrders(){
        ArrayList<Ordem> resultList;
        ArrayList<Ordem> totalOrdersTeste;
        ArrayList <Ordem> partialOrdersTeste;
        Ordem ordemTeste;
        ordemTeste=new Ordem (50,100, LocalTime.of(14,30),3,"ATV001","compra","limitada",
                "456.735.487-90", "pendente");
        totalOrdersTeste= new ArrayList<Ordem>();
        totalOrdersTeste.add(new Ordem (40,13,LocalTime.of(14,30),1,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (35,22,LocalTime.of(14,30),2,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (45,130,LocalTime.of(14,30),3,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (32,88,LocalTime.of(14,30),4,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (33,57,LocalTime.of(14,30),5,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (37,44,LocalTime.of(14,30),6,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (44,200,LocalTime.of(14,30),7,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        totalOrdersTeste.add(new Ordem (36,500,LocalTime.of(14,30),8,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste=new ArrayList<>();
        partialOrdersTeste.add(new Ordem (33,1,LocalTime.of(14,30),10,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste.add(new Ordem (37,3,LocalTime.of(14,30),11,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste.add(new Ordem (44,6,LocalTime.of(14,30),12,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));
        partialOrdersTeste.add(new Ordem (36,500,LocalTime.of(14,30),13,"ATV001","venda","limitada",
                "456.735.487-90", "pendente"));

        resultList= HomeBroker.getBestMixedOrdersTransaction(ordemTeste.quantity,partialOrdersTeste,totalOrdersTeste);
        assertEquals(6,resultList.get(0).id);
        assertEquals(10,resultList.get(1).id);
        assertEquals(11,resultList.get(2).id);
        assertEquals(12,resultList.get(3).id);
        assertEquals(13,resultList.get(4).id);
        assertEquals(5,resultList.size());
        assertEquals(46,resultList.get(4).quantity);

    }

}
