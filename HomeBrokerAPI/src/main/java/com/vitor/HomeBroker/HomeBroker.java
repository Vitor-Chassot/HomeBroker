
package com.vitor.HomeBroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cglib.core.Local;

import java.sql.*;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.time.LocalTime;

@SpringBootApplication
public class HomeBroker {
    static Connection conn;
    public static void main(String[] args) {

        SpringApplication.run(HomeBroker.class, args);

        long startTime = System.nanoTime();
        // Configurações do banco
        String url = "jdbc:postgresql://localhost:5432/homebroker"; // Se estiver rodando o Spring Boot no IntelliJ
        String user = "admin";
        String password = "admin";
        //contectar com o DB
        conn=null;

        try{
            conn=DriverManager.getConnection(url, user, password);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("Digite o ID da ordem:");
        int id = scanner.nextInt();
        scanner.nextLine(); // Consumir quebra de linha

        System.out.println("Digite o código do investidor:");
        String codInvestidor = scanner.nextLine();

        System.out.println("Digite o código do ativo:");
        String codAtivo = scanner.nextLine();

        System.out.println("Digite o tipo da ordem (compra/venda):");
        String tipo = scanner.nextLine();

        System.out.println("Digite o status da ordem (cancelada/pendente/realizada):");
        String status = scanner.nextLine();

        System.out.println("Digite a quantidade:");
        int quantidade = scanner.nextInt();
        scanner.nextLine(); // Consumir quebra de linha

        System.out.println("Digite o modo da ordem (limitada/mercado):");
        String modo = scanner.nextLine();

        System.out.println("Digite a execução da ordem (parcial/total):");
        String execucao = scanner.nextLine();

        System.out.println("Digite a data de criação no formato yyyy-MM-ddTHH:mm:ss");
        String dataCriacao = scanner.nextLine();

        // Chama o método para inserir no banco
        inserirOrdem(id, codInvestidor, codAtivo, tipo, status, quantidade, modo, execucao, dataCriacao);

        System.out.println("Ordem cadastrada com sucesso!");
        scanner.close();
        realizaTransacao();

    }

    public static void inserirOrdem(int id, String codInvestidor, String codAtivo, String tipo, String status, int quantidade, String modo, String execucao, String dataCriacao) {
        String query = "INSERT INTO ORDEM (COD_ORDEM, COD_INVESTIDOR, COD_ATIVO, TIPO, STATUS, QUANTIDADE, MODO, EXECUCAO, DAT_CRIACAO) " +
                "VALUES (?, ?, ?, ?::tipo_ordem, ?::status_ordem, ?, ?::modo_ordem, ?::execucao_ordem, ?)";
        PreparedStatement stmt = null;

        try  {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            stmt.setString(2, codInvestidor);
            stmt.setString(3, codAtivo);
            stmt.setString(4, tipo);   // Mantém o valor como foi recebido
            stmt.setString(5, status); // Mantém o valor como foi recebido
            stmt.setInt(6, quantidade);
            stmt.setString(7, modo);   // Mantém o valor como foi recebido
            stmt.setString(8, execucao); // Mantém o valor como foi recebido
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.parse(dataCriacao))); // Converte String para Timestamp

            stmt.executeUpdate();
            System.out.println("Ordem inserida com sucesso!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void realizaTransacao() {
        System.out.println("Vendo transacoes possiveis");
        Ordem actualOrder = null;
        String execucao = null;

        // Query para obter a última ordem inserida
        String query = "SELECT * FROM ORDEM ORDER BY DAT_CRIACAO DESC LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int id = rs.getInt("COD_ORDEM");
                String idAtivo = rs.getString("COD_ATIVO");
                String tipo = rs.getString("TIPO");
                String modo = rs.getString("MODO");
                String idInv = rs.getString("COD_INVESTIDOR");
                String status = rs.getString("STATUS");
                int quantidade = rs.getInt("QUANTIDADE");
                double priceLim = 0.0; // Definir preço limite se necessário

                LocalTime time = rs.getTimestamp("DAT_CRIACAO").toLocalDateTime().toLocalTime();

                actualOrder = new Ordem(priceLim, quantidade, time, id, idAtivo, tipo, modo, idInv, status);
                execucao = rs.getString("EXECUCAO"); // Guardar a string de execução
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (actualOrder != null) {
            System.out.println("Ultima Ordem Encontrada");
            // Supondo que getCompatibleOrdersBytypeAndExecution existe
            ArrayList<Ordem> list = getBestTransaction(actualOrder, execucao);
            if(list==null||list.size()==0)
                System.out.println("Nenhuma transacao encontrada");
            else
                    for (Ordem ordem : list) {
                System.out.println("ID: " + ordem.id +
                        ", Ativo: " + ordem.idAtivo +
                        ", Preço: " + ordem.priceLim +
                        ", Quantidade: " + ordem.quantity +
                        ", Tipo: " + ordem.tipo +
                        ", Modo: " + ordem.modo +
                        ", IdInv: " + ordem.idInv);
                    }
        } else {
            System.out.println("Nenhuma ordem encontrada.");
        }
    }

    public static ArrayList<Ordem> getCompatibleOrdersBytypeAndExecution(Ordem actualOrder, String type, String exec) {
        ArrayList<Ordem> ordens = new ArrayList<>();
        String query = "SELECT * FROM ORDEM O "
                + "LEFT JOIN ORDEM_LIMITADA OL ON O.COD_ORDEM = OL.COD_ORDEM "
                + "WHERE O.COD_ATIVO = ? "
                + "AND O.COD_INVESTIDOR != ? "
                + "AND DATE(O.DAT_CRIACAO) = DATE(NOW()) "
                + "AND O.TIPO::VARCHAR = ? "  // Cast explícito para VARCHAR
                + "AND O.EXECUCAO::VARCHAR= ? ";
                if(actualOrder.modo.equals("limitada")) {
                    query=  query
                            + "AND (OL.PRECO_LIM IS NULL OR OL.PRECO_LIM "
                            + (type.equals("compra") ? ">= ?" : "<= ?)")
                            + ") ORDER BY O.DAT_CRIACAO ASC";
                }



        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(query);

            // Definindo os parâmetros para a consulta
            stmt.setString(1, actualOrder.idAtivo);  // Primeiro parâmetro: COD_ATIVO
            stmt.setString(3, type);  // Segundo parâmetro: TIPO (passando o valor da string 'type' diretamente)
            stmt.setString(2, actualOrder.idInv);
            stmt.setString(4, exec);  // Terceiro parâmetro: EXECUCAO
            if(actualOrder.modo.equals("limitada"))
                stmt.setDouble(5, actualOrder.priceLim);  // Quarto parâmetro: PRECO_LIM (dependendo de tipo 'compra' ou 'venda')
            rs = stmt.executeQuery();
            while(rs.next()){
                int id = rs.getInt("COD_ORDEM");
                double priceLim = (rs.getObject("PRECO_LIM") != null)
                        ? rs.getDouble("PRECO_LIM")
                        : (type.equals("compra") ? Double.MAX_VALUE : 0.0);
                int quantity = rs.getInt("QUANTIDADE");
                LocalTime time = rs.getTimestamp("DAT_CRIACAO").toLocalDateTime().toLocalTime();
                String idAtivo = rs.getString("COD_ATIVO");
                String tipo = rs.getString("TIPO");
                String modo = rs.getString("MODO");
                String idInv = rs.getString("COD_INVESTIDOR");
                String status = rs.getString("STATUS");
                Ordem ordem = new Ordem(priceLim, quantity, time, id, idAtivo, tipo, modo,idInv,status);
                ordens.add(ordem);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }



        // Retornar a query formatada corretamente
        return ordens;
    }

    public static ArrayList<Ordem> getBestTransaction(Ordem actualOrder,String exec){
        String same,opos;
        same=actualOrder.tipo.equals("compra")?"compra":"venda";
        opos=actualOrder.tipo.equals("compra")?"venda":"compra";
        ArrayList<Ordem> totalOppositeOrders=getCompatibleOrdersBytypeAndExecution(actualOrder, opos, "total");
        ArrayList<Ordem> partialOppositeOrders=getCompatibleOrdersBytypeAndExecution(actualOrder,opos,"parcial");
        ArrayList<Ordem> totalSameOrders=getCompatibleOrdersBytypeAndExecution(actualOrder,same,"total");
        ArrayList<Ordem> partialSameOrders=getCompatibleOrdersBytypeAndExecution(actualOrder,same,"parcial");
        //ArrayList<Ordem> sameTypeCombination = getBestTransactionWithSametypeCombination(actualOrder, totalOppositeOrders, partialOppositeOrders, totalSameOrders, partialSameOrders);
        if (exec.equals("total"))
            return getBestTransactionWithOppositeTypeCombinationForTotalOrder(actualOrder, partialOppositeOrders,totalOppositeOrders );
        else
            return getBestTransactionWithOppositeTypeCombinationForPartialOrder(actualOrder, partialOppositeOrders,totalOppositeOrders );

    }
    public static ArrayList<Ordem> getBestTransactionWithOppositeTypeCombinationForPartialOrder(Ordem actualOrder, ArrayList<Ordem> partialOppositeOrders,ArrayList<Ordem> totalOppositeOrders){
        int i=0,j=0;
        int partialLength=partialOppositeOrders.size();
        int totalLength=totalOppositeOrders.size();
        int currentSum=0;
        boolean totalTurn=true;
        boolean lastTotal=false;
        ArrayList<Ordem> bestTransation=new ArrayList<Ordem>();
        while(j<partialLength&&i<totalLength&&currentSum<actualOrder.quantity){
            if(totalTurn){
                bestTransation.add(totalOppositeOrders.get(i));
                currentSum+=totalOppositeOrders.get(i).quantity;
                lastTotal=true;
                i++;
            }
            else{
                bestTransation.add(partialOppositeOrders.get(j));
                currentSum+=partialOppositeOrders.get(j).quantity;
                lastTotal=false;
                j++;
            }
            totalTurn=!totalTurn;

        }
        if(currentSum<actualOrder.quantity){
            while(j<partialLength&&currentSum<actualOrder.quantity){
                bestTransation.add(partialOppositeOrders.get(j));
                currentSum+=partialOppositeOrders.get(j).quantity;
                lastTotal=false;
                j++;
            }
            while(i<totalLength&&currentSum<actualOrder.quantity){
                bestTransation.add(totalOppositeOrders.get(i));
                currentSum+=totalOppositeOrders.get(i).quantity;
                lastTotal=true;
                i++;
            }
        }
        if(currentSum==actualOrder.quantity)
            return bestTransation;
        else{
            bestTransation.remove(bestTransation.size()-1);
            return bestTransation;
        }





    }
    public static ArrayList<Ordem> getTransactionWithOldestOffers(ArrayList<Ordem> sameTypeCombination,ArrayList<Ordem> oppositeTypeCombination){
        LocalTime lastestSameTypeTime=null;
        for(Ordem ordem : sameTypeCombination){
            if(ordem.time.isAfter(lastestSameTypeTime)){
                lastestSameTypeTime = ordem.time;
            }
        }
        LocalTime latestOppositeTypeTime=null;
        for(Ordem ordem : oppositeTypeCombination){
            if(ordem.time.isAfter(latestOppositeTypeTime)){
                latestOppositeTypeTime = ordem.time;
            }
        }
        return latestOppositeTypeTime.isAfter(lastestSameTypeTime) ? sameTypeCombination : oppositeTypeCombination;
    }
    public static ArrayList<Ordem> getTransactionWithAlmostAllTotalOrders(int quantity, ArrayList<Ordem> totalOppositeOrders,ArrayList<Ordem> partialOppositeOrders) {
        LocalTime lastTime=null;
        ArrayList<Ordem> bestTotalTransaction=null;
        int lengthTotal=totalOppositeOrders.size();
        int closestSum=0;
        int currentSum=0;

        for(int i=0; i<lengthTotal; i++){
            currentSum=totalOppositeOrders.get(i).quantity;
            if((currentSum>closestSum&&currentSum<=quantity)
            ||(lastTime!=null&&(currentSum==closestSum)&&(totalOppositeOrders.get(i).time.isBefore(lastTime)))){
                bestTotalTransaction=new ArrayList<>();
                bestTotalTransaction.add(totalOppositeOrders.get(i));
                lastTime=totalOppositeOrders.get(i).time;
                closestSum=currentSum;
            }
        }
        for(int i=0; i<lengthTotal; i++) {
            for (int j = i + 1; j < lengthTotal; j++) {
                currentSum=totalOppositeOrders.get(i).quantity+totalOppositeOrders.get(j).quantity;
                if (currentSum>closestSum&&currentSum<=quantity ||(lastTime!=null&&currentSum==closestSum&&
                        (totalOppositeOrders.get(i).time.isBefore(lastTime)||totalOppositeOrders.get(j).time.isBefore(lastTime)))){
                    bestTotalTransaction = new ArrayList<>();
                    bestTotalTransaction.add(totalOppositeOrders.get(i));
                    bestTotalTransaction.add(totalOppositeOrders.get(j));
                    closestSum=currentSum;
                    lastTime = totalOppositeOrders.get(i).time.isAfter(totalOppositeOrders.get(j).time) ? totalOppositeOrders.get(i).time : totalOppositeOrders.get(j).time;

                }
            }

        }
        for(int i=0; i<lengthTotal; i++){
            for(int j=i+1; j<lengthTotal; j++){
                for(int k=j+1; k<lengthTotal; k++){

                    currentSum=totalOppositeOrders.get(i).quantity+totalOppositeOrders.get(j).quantity
                    +totalOppositeOrders.get(k).quantity;
                    if (currentSum>closestSum&&currentSum<=quantity ||(lastTime!=null&&currentSum==closestSum&&
                            (totalOppositeOrders.get(i).time.isBefore(lastTime)||totalOppositeOrders.get(j).time.isBefore(lastTime)
                            ||totalOppositeOrders.get(k).time.isBefore(lastTime)))){
                        bestTotalTransaction = new ArrayList<>();
                        bestTotalTransaction.add(totalOppositeOrders.get(i));
                        bestTotalTransaction.add(totalOppositeOrders.get(j));
                        bestTotalTransaction.add(totalOppositeOrders.get(k));
                        closestSum=currentSum;
                        lastTime = totalOppositeOrders.get(i).time.isBefore(totalOppositeOrders.get(j).time) ? totalOppositeOrders.get(i).time : totalOppositeOrders.get(j).time;
                        lastTime=totalOppositeOrders.get(k).time.isBefore(lastTime)?totalOppositeOrders.get(k).time : lastTime;

                    }

                }


            }

        }
        if(currentSum==quantity)
            return bestTotalTransaction;
        int remainingQuantity=quantity-closestSum;
        ArrayList<Ordem> partialOrdersForTransaction=getRemainingQuantityFromPartialOrders(remainingQuantity,partialOppositeOrders);
        if(partialOrdersForTransaction==null)
            return null;
        bestTotalTransaction.addAll(partialOrdersForTransaction);
        return bestTotalTransaction;
    }
    public static ArrayList<Ordem> getRemainingQuantityFromPartialOrders(int quantity,ArrayList<Ordem> partialOppositeOrders) {
        if(partialOppositeOrders==null)
            return null;
        ArrayList<Ordem> partialOrdersForTransaction = new ArrayList<>();
        int lengthPartial=partialOppositeOrders.size();
        int sumQuantity=0;
        int i=0;
        while(sumQuantity<quantity&&i<lengthPartial){
            partialOrdersForTransaction.add(partialOppositeOrders.get(i));
            sumQuantity+=partialOppositeOrders.get(i).quantity;
            i++;
        }
        if(sumQuantity<quantity){
            partialOrdersForTransaction=null;
        }
        if(sumQuantity>quantity){
            Ordem lastOrder=partialOrdersForTransaction.get(partialOrdersForTransaction.size()-1);
            partialOrdersForTransaction.remove(partialOrdersForTransaction.size()-1);
            sumQuantity-=lastOrder.quantity;
            int remainingQuantity=quantity-sumQuantity;
            Ordem newLastOrder=new Ordem(lastOrder.priceLim, remainingQuantity,lastOrder.time, lastOrder.id, lastOrder.idAtivo, lastOrder.tipo,
                    lastOrder.modo, lastOrder.idInv, lastOrder.status);
            partialOrdersForTransaction.add(newLastOrder);


        }
        return partialOrdersForTransaction;
    }
    public static ArrayList<Ordem> getBestMixedOrdersTransaction(int quantity,ArrayList<Ordem> partialOppositeOrders,ArrayList<Ordem> totalOppositeOrders) {
        int lengthPartial=partialOppositeOrders.size();
        int lengthTotal=totalOppositeOrders.size();
        int sumPartialQuantity=0;
        int indPar=0;
        ArrayList<Ordem> auxTotalOrders=new ArrayList<>();
        ArrayList<Ordem> auxPartialOrders=new ArrayList<>();
        while(sumPartialQuantity<quantity&&indPar<lengthPartial){
            auxPartialOrders.add(partialOppositeOrders.get(indPar));
            sumPartialQuantity+=partialOppositeOrders.get(indPar).quantity;
            indPar++;
        }
        int remainingQuantity=0;
        if(sumPartialQuantity<quantity){
            remainingQuantity=quantity-sumPartialQuantity;
        }
        int sumTotalQuantity=0;
        int sumActualTotalQuantity=0;
        int actualEpsilon=Integer.MAX_VALUE;
        for(int i=0; i<lengthTotal; i++){
            sumActualTotalQuantity=totalOppositeOrders.get(i).quantity;
            int newEpsilon=Math.abs((quantity/2)-sumActualTotalQuantity);
            if(sumActualTotalQuantity>=remainingQuantity&&newEpsilon<actualEpsilon){
                auxTotalOrders=new ArrayList<>();
                auxTotalOrders.add(totalOppositeOrders.get(i));
                actualEpsilon=newEpsilon;
                sumTotalQuantity=sumActualTotalQuantity;

            }
        }
        for(int i=0; i<lengthTotal; i++){
            for(int j=i+1; j<lengthTotal; j++){
                sumActualTotalQuantity=totalOppositeOrders.get(i).quantity+totalOppositeOrders.get(j).quantity;
                int newEpsilon=Math.abs((quantity/2)-sumActualTotalQuantity);
                if(sumActualTotalQuantity>=remainingQuantity&&newEpsilon<actualEpsilon){
                    auxTotalOrders=new ArrayList<>();
                    auxTotalOrders.add(totalOppositeOrders.get(i));
                    auxTotalOrders.add(totalOppositeOrders.get(j));
                    actualEpsilon=newEpsilon;
                    sumTotalQuantity=sumActualTotalQuantity;

                }



            }

        }
        for(int i=0; i<lengthTotal; i++){
            for(int j=i+1; j<lengthTotal; j++){
                for(int k=j+1; k<lengthTotal; k++){
                    sumActualTotalQuantity=totalOppositeOrders.get(i).quantity+totalOppositeOrders.get(j).quantity
                            +totalOppositeOrders.get(k).quantity;
                    int newEpsilon=Math.abs((quantity/2)-sumActualTotalQuantity);
                    if(sumActualTotalQuantity>=remainingQuantity&&newEpsilon<actualEpsilon){
                        auxTotalOrders=new ArrayList<>();
                        auxTotalOrders.add(totalOppositeOrders.get(i));
                        auxTotalOrders.add(totalOppositeOrders.get(j));
                        auxTotalOrders.add(totalOppositeOrders.get(k));
                        actualEpsilon=newEpsilon;
                        sumTotalQuantity=sumActualTotalQuantity;

                    }


                }




            }

        }
        remainingQuantity=quantity-sumTotalQuantity;
        auxPartialOrders=new ArrayList<>();
        sumPartialQuantity=0;
        int i=0;
        while(sumPartialQuantity<remainingQuantity&&i<lengthPartial){
            auxPartialOrders.add(partialOppositeOrders.get(i));
            sumPartialQuantity+=partialOppositeOrders.get(i).quantity;
            i++;
        }
        if(sumPartialQuantity<remainingQuantity){
            auxPartialOrders=null;
        }
        else if(sumPartialQuantity>remainingQuantity){
            Ordem lastOrder=auxPartialOrders.get(auxPartialOrders.size()-1);
            auxPartialOrders.remove(auxPartialOrders.size()-1);
            sumPartialQuantity-=lastOrder.quantity;
            remainingQuantity-=sumPartialQuantity;
            Ordem newLastOrder=new Ordem(lastOrder.priceLim, remainingQuantity,lastOrder.time, lastOrder.id, lastOrder.idAtivo, lastOrder.tipo,
                    lastOrder.modo, lastOrder.idInv, lastOrder.status);
            auxPartialOrders.add(newLastOrder);


        }
        ArrayList<Ordem> bestMixedTransaction=new ArrayList<>(auxTotalOrders);
        if(auxPartialOrders!=null&&auxTotalOrders.size()>0&&auxPartialOrders.size()>0){
            bestMixedTransaction.addAll(auxPartialOrders);
            return bestMixedTransaction;
        }
        else if(auxTotalOrders.size()>0){
            return auxPartialOrders;
        }
        else if(auxPartialOrders!=null&&auxPartialOrders.size()>0){
            return auxPartialOrders;
        }
        else
            return null;


    }
    public static ArrayList<Ordem> getBestTransactionWithOppositeTypeCombinationForTotalOrder(Ordem totalOrder,ArrayList<Ordem> totalOppositeOrders,ArrayList<Ordem> partialOppositeOrders){
        ArrayList<Ordem> bestTransaction;
        ArrayList<Ordem> bestTotalTransaction;
        ArrayList<Ordem> bestPartialTransaction;
        ArrayList<Ordem> bestMixedTransaction;
        LocalTime lastTime=null;
        int lengthPartial=partialOppositeOrders.size();
        int lengthTotal=totalOppositeOrders.size();

        bestTotalTransaction=getTransactionWithAlmostAllTotalOrders(totalOrder.quantity,totalOppositeOrders,partialOppositeOrders);
        bestPartialTransaction=getRemainingQuantityFromPartialOrders(totalOrder.quantity,partialOppositeOrders);
        bestMixedTransaction=getBestMixedOrdersTransaction(totalOrder.quantity,partialOppositeOrders,totalOppositeOrders);

        if(lengthTotal>(2*lengthPartial)||(bestTotalTransaction==null&&bestMixedTransaction==null))
            return bestPartialTransaction;
        else if(lengthPartial>(2*lengthTotal)||(bestPartialTransaction==null&&bestMixedTransaction==null))
            return bestTotalTransaction;
        else
            return bestMixedTransaction;




    }
    public static ArrayList<Ordem> getBestTransactionWithSametypeCombination(Ordem actualOrder,ArrayList<Ordem> totalOppositeOrders,ArrayList<Ordem> partialOppositeOrders,
    ArrayList<Ordem> totalSameOrders,ArrayList<Ordem> partialSameOrders){
        ArrayList<Ordem> ordens = new ArrayList<>();
        int oposTotalInd=0;
        int oposPartialInd=0;
        int oposTotalLength=totalOppositeOrders.size();
        int oposPartialLength=partialOppositeOrders.size();
        boolean lastWasTotal=false;
        boolean actualIsTotal=false;
        Ordem actualOppositeOrder;
        while(oposTotalInd<oposTotalInd&&oposPartialInd<oposPartialLength){
            if(totalOppositeOrders.get(oposTotalInd).time.isBefore(partialOppositeOrders.get(oposPartialInd).time))
                actualIsTotal=true;
            if(totalOppositeOrders.get(oposTotalInd).time.isAfter(partialOppositeOrders.get(oposPartialInd).time))
                actualIsTotal=false;
            else
                actualIsTotal=lastWasTotal?false:true;
            if(actualIsTotal){
                actualOppositeOrder=totalOppositeOrders.get(oposTotalInd);
                oposTotalInd++;
            }
            else{
                actualOppositeOrder=partialOppositeOrders.get(oposPartialInd);
                oposPartialInd++;
            }
            ordens=getBestTransactionForCurrentOppositeOffer(actualOrder,actualOppositeOrder,totalSameOrders,partialSameOrders);
            if(ordens!=null)
                break;
            lastWasTotal=actualIsTotal;

        }
        return ordens;

    }

    public static ArrayList<Ordem> getBestTransactionForCurrentOppositeOffer(Ordem actualOrder,Ordem actualOppositeOrder,ArrayList<Ordem> totalSameOrders,ArrayList<Ordem> partialSameOrders){
        return null;
    }

}

class Ordem{
    public Ordem(double priceLim,int quantity,LocalTime time,int id,String idAtivo,
                 String tipo,String modo,String idInv,String status){
        this.priceLim = priceLim;
        this.quantity = quantity;
        this.time = time;
        this.id=id;
        this.idAtivo=idAtivo;
        this.tipo=tipo;
        this.modo=modo;
        this.idInv=idInv;
        this.status=status;
    }
    int id;
    double priceLim;
    int quantity;
    LocalTime time;
    String status;
    String idAtivo;
    String tipo;
    String modo;
    String idInv;

}






