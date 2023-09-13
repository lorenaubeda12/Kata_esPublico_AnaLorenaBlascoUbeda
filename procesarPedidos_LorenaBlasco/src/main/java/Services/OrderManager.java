package Services;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import Utils.SQLConnection;
import com.opencsv.CSVReader;
import POJO.Order;

import org.apache.commons.io.FilenameUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OrderManager {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final int REGION_COLUMN = 0;
    private static final int COUNTRY_COLUMN = 1;
    private static final int ITEM_TYPE_COLUMN = 2;
    private static final int SALES_CHANNEL_COLUMN = 3;
    private static final int PRIORITY_COLUMN = 4;
    private static final int DATE_COLUMN = 5;
    private static final int ORDER_ID_COLUMN = 6;
    private static final int SHIP_DATE_COLUMN = 7;
    private static final int UNITS_SOLD_COLUMN = 8;
    private static final int UNIT_PRICE_COLUMN = 9;
    private static final int UNIT_COST_COLUMN = 10;
    private static final int TOTAL_REVENUE_COLUMN = 11;
    private static final int TOTAL_COST_COLUMN = 12;
    private static final int TOTAL_PROFIT_COLUMN = 13;
    private static final String SQL_INSERT_QUERY = "INSERT INTO orders (order_ID, order_Priority, order_Date, Region, Country, ItemType, SalesChannel, ShipDate, UnitsSold, UnitPrice, UnitCost, TotalRevenue, TotalCost, TotalProfit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Connection connectDB = SQLConnection.openConnection()) {
            System.out.println("Conexion correcta");
            try {
                System.out.println("Bienvenido a la digitalización de datos.");
                System.out.println("Introduzca la ruta del archivo CSV");
                String pathCSV = scanner.nextLine();
                if (extensionComprobation(pathCSV, "csv")) {
                    try (CSVReader csvReader = new CSVReader(new FileReader(pathCSV))) {
                        List<Order> lstOrders = new ArrayList<>();
                        skipHeaders(csvReader);
                        List<String[]> lines = csvReader.readAll();
                        lines.forEach(line -> {
                            Order newOrder = extractOrderFromLine(line);
                            lstOrders.add(newOrder);
                        });

                        insertOrdersToDatabase(lstOrders, connectDB);

                        // Genera un archivo CSV ordenado por orderId
                        sortOrdersById(lstOrders);
                        String savePath = "";
                        do {
                            System.out.println("Introduzca la ruta donde guardar el archivo generado, así como su nombre. \n Ejemplo: C:\\Prueba\\archivo.csv");
                            savePath = scanner.nextLine();
                        } while (savePath.isEmpty());
                        exportDataToNewCSV(lstOrders, savePath);

                        // Muestra el resumen del conteo por tipo
                        showSummary(lstOrders);

                    } catch (Exception exception) {
                        exception.printStackTrace();
                        System.out.println("Ha ocurrido un error al intentar leer los datos");
                    }
                } else {
                    System.out.println("El archivo a leer no es un .CSV, solo aceptamos este formato de archivo");
                }


            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                SQLConnection.closeConnection(connectDB);
                scanner.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("Ha ocurrido un error al intentar conectar con la base de datos");
        }
    }

    private static void sortOrdersById(List<Order> lstOrders) {
        Collections.sort(lstOrders, Comparator.comparing(Order::getOrderID));
    }

    private static Order extractOrderFromLine(String[] line) {
        Order newOrder = new Order();
        newOrder.setRegion(line[REGION_COLUMN]);
        newOrder.setCountry(line[COUNTRY_COLUMN]);
        newOrder.setItemType(line[ITEM_TYPE_COLUMN]);
        newOrder.setSalesChannel(line[SALES_CHANNEL_COLUMN]);
        newOrder.setOrderPriority(line[PRIORITY_COLUMN]);
        newOrder.setOrderDate(line[DATE_COLUMN]);
        newOrder.setOrderID(line[ORDER_ID_COLUMN]);
        newOrder.setShipDate(line[SHIP_DATE_COLUMN]);
        newOrder.setUnitsSold(Integer.parseInt(line[UNITS_SOLD_COLUMN]));
        newOrder.setUnitPrice(Double.parseDouble(line[UNIT_PRICE_COLUMN]));
        newOrder.setUnitCost(Double.parseDouble(line[UNIT_COST_COLUMN]));
        newOrder.setTotalRevenue(Double.parseDouble(line[TOTAL_REVENUE_COLUMN]));
        newOrder.setTotalCost(Double.parseDouble(line[TOTAL_COST_COLUMN]));
        newOrder.setTotalProfit(Double.parseDouble(line[TOTAL_PROFIT_COLUMN]));
        return newOrder;
    }

    private static void skipHeaders(CSVReader csvReader) throws IOException, CsvValidationException {
        csvReader.readNext();
    }


    public static boolean extensionComprobation(String path, String expectedExtension) {
        String extension = FilenameUtils.getExtension(path).toLowerCase();
        if (extension.equals(expectedExtension)) {
            return true;
        } else {
            return false;
        }
    }

    public static void insertOrdersToDatabase(List<Order> orders, Connection connection) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_QUERY)) {
                int batchSize = 0;
                int totalInsertData = 0;
                System.out.println("Insertando datos en la base de datos. Por favor, espere.");
                for (Order order : orders) {
                    addOrderToBatch(preparedStatement, order);
                    batchSize++;


                    if (batchSize % MAX_BATCH_SIZE == 0) {
                        // Si se ha alcanzado el tamaño del lote (1000), ejecuta el lote y restablece el contador

                        int[] batchResult = preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                        totalInsertData += batchSize;
                        batchSize = 0;
                    }

                    System.out.println(totalInsertData + " datos introducidos de un total de " + orders.size());
                }

                // Ejecutar el lote final (si no se ha alcanzado exactamente 1000 registros)
                if (batchSize > 0) {
                    int[] batchResult = preparedStatement.executeBatch();
                }
                System.out.println("Total de datos insertados: " + totalInsertData);
                // Confirmar la transacción si todas las inserciones se realizaron con éxito
                connection.commit();

                System.out.println("Todos los datos han sido añadidos de manera correcta a la base de datos.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Error al insertar datos en la base de datos. Realizando rollback.");

                // En caso de error, realizar un rollback para deshacer todas las inserciones
                connection.rollback();
            } finally {
                // Restaurar el autocommit a su estado original
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error al deshabilitar el autocommit.");
        }
    }

    private static void addOrderToBatch(PreparedStatement preparedStatement, Order order) throws SQLException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        preparedStatement.setString(1, order.getOrderID());
        preparedStatement.setString(2, order.getOrderPriority());

        Date utilOrderDate = order.getOrderDate();
        java.sql.Date sqlOrderDate = new java.sql.Date(utilOrderDate.getTime());
        preparedStatement.setDate(3, sqlOrderDate);


        preparedStatement.setString(4, order.getRegion());
        preparedStatement.setString(5, order.getCountry());
        preparedStatement.setString(6, order.getItemType());
        preparedStatement.setString(7, order.getSalesChannel());

        Date utilShipDate = order.getShipDate();
        sqlOrderDate = new java.sql.Date(utilShipDate.getTime());

        preparedStatement.setDate(8, sqlOrderDate);
        preparedStatement.setInt(9, order.getUnitsSold());
        preparedStatement.setDouble(10, order.getUnitPrice());
        preparedStatement.setDouble(11, order.getUnitCost());
        preparedStatement.setDouble(12, order.getTotalRevenue());
        preparedStatement.setDouble(13, order.getTotalCost());
        preparedStatement.setDouble(14, order.getTotalProfit());

        preparedStatement.addBatch();
    }


    public static void exportDataToNewCSV(List<Order> orders, String filename) {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filename),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.RFC4180_LINE_END)) {

            // Escribe el encabezado en el archivo CSV
            String[] header = {
                    "Order ID",
                    "Order Priority",
                    "Order Date",
                    "Region",
                    "Country",
                    "Item Type",
                    "Sales Channel",
                    "Ship Date",
                    "Units Sold",
                    "Unit Price",
                    "Unit Cost",
                    "Total Revenue",
                    "Total Cost",
                    "Total Profit"
            };
            csvWriter.writeNext(header);

            // Recopila los datos a escribir en el archivo CSV
            List<String[]> orderLines = extractOrderLines(orders);
            // Escribe todas las lineas de pedido recogidas en el archivo CSV
            csvWriter.writeAll(orderLines);
            System.out.println("Los datos se han exportado correctamente al archivo " + filename);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al exportar datos al archivo CSV.");
        }
    }

    private static List<String[]> extractOrderLines(List<Order> orders) {
        List<String[]> orderLines = new ArrayList<>();
        orders.forEach(order -> {
            String[] data = extractOrderData(order);
            orderLines.add(data);
        });
        return orderLines;
    }

    private static String[] extractOrderData(Order order) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String orderDateParse = dateFormat.format(order.getOrderDate());
        String shipDateParse = dateFormat.format(order.getShipDate());

        String[] data = {
                order.getOrderID(),
                order.getOrderPriority(),
                orderDateParse,
                order.getRegion(),
                order.getCountry(),
                order.getItemType(),
                order.getSalesChannel(),
                shipDateParse,
                String.valueOf(order.getUnitsSold()),
                String.valueOf(order.getUnitPrice()),
                String.valueOf(order.getUnitCost()),
                String.valueOf(order.getTotalRevenue()),
                String.valueOf(order.getTotalCost()),
                String.valueOf(order.getTotalProfit())
        };

        return data;
    }


    public static void showSummary(List<Order> orders) {
        Map<String, Integer> regionCount = new HashMap<>();
        Map<String, Integer> countryCount = new HashMap<>();
        Map<String, Integer> itemTypeCount = new HashMap<>();
        Map<String, Integer> salesChannelCount = new HashMap<>();
        Map<String, Integer> orderPriorityCount = new HashMap<>();

        //Conteo por region,pais,tipo de item, canal de ventas, prioridad...
        for (Order order : orders) {

            String region = order.getRegion();
            regionCount.put(region, regionCount.getOrDefault(region, 0) + 1);

            String country = order.getCountry();
            countryCount.put(country, countryCount.getOrDefault(country, 0) + 1);

            String itemType = order.getItemType();
            itemTypeCount.put(itemType, itemTypeCount.getOrDefault(itemType, 0) + 1);

            String salesChannel = order.getSalesChannel();
            salesChannelCount.put(salesChannel, salesChannelCount.getOrDefault(salesChannel, 0) + 1);

            String orderPriority = order.getOrderPriority();
            orderPriorityCount.put(orderPriority, orderPriorityCount.getOrDefault(orderPriority, 0) + 1);

        }
        System.out.println("------------------------------");
        System.out.println("Conteo por región");
        showCounts(regionCount);
        System.out.println("------------------------------\n");

        System.out.println("Conteo por país");
        showCounts(countryCount);
        System.out.println("------------------------------\n");

        System.out.println("Conteo por tipo de item:");
        showCounts(itemTypeCount);
        System.out.println("------------------------------\n");
        System.out.println("Conteo por canal de ventas");
        showCounts(salesChannelCount);
        System.out.println("------------------------------\n");

        System.out.println("Conteo por por prioridad de pedido:");
        showCounts(orderPriorityCount);
        System.out.println("------------------------------\n");

    }


    public static void showCounts(Map<String, Integer> count) {
        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}

