import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class Base {
    private int salud;
    private int x, y;
    private BufferedImage image;

    private GamePanel panel;

    public Base(int salud, int x, int y, String imagePath, GamePanel panel) {
        this.salud = salud;
        this.x = x;
        this.y = y;
        this.panel = panel;
        try {
            this.image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
            this.image = null;
        }
    }

    public int getSalud() {
        return salud;
    }

    public void recibirDaño(int daño) {
        if (!panel.isJuegoActivo()) {
            return;
        }

        salud -= daño;
        System.out.println("Daño sufrido: " + salud);
        if (salud <= 0) {
            System.out.println("La base ha sido destruida. Game Over!");
            panel.setJuegoActivo(false);
            JOptionPane.showMessageDialog(null, "Game Over! Restarting the game...");
            SwingUtilities.invokeLater(() -> panel.resetGame());
        }
    }

    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, x, y, 100, 100, null); // Dibuja la imagen con tamaño fijo
        }
    }

    // Getters para la posición
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

class Bala {
    private int x, y, dx, dy;
    private int daño;
    private boolean congelante;
    private BufferedImage image;

    public Bala(int x, int y, int dx, int dy, int daño, boolean congelante, String imagePath) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.daño = daño;
        this.congelante = congelante;
        try {
            this.image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
            this.image = null;
        }
    }

    public void mover() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, x, y, 30, 30, null);
        }
    }

    public boolean impacto(Enemigo enemigo) {
        // Simple chequeo de colisión
        return Math.hypot(x - enemigo.getX(), y - enemigo.getY()) < 10;
    }

    public boolean isCongelante() {
        return congelante;
    }

    public int getDaño() {
        return daño;
    }
}

abstract class Torre {
    protected int daño;
    protected int rango;
    protected int x, y;
    protected BufferedImage image;
    protected List<Bala> balas = new ArrayList<>();
    protected long lastShotTime = 0; // Tiempo del último disparo
    protected int shotInterval; // Intervalo mínimo entre disparos en milisegundos

    public Torre(int daño, int rango, int x, int y, int shotInterval, String imagePath) {
        this.daño = daño;
        this.rango = rango;
        this.x = x;
        this.y = y;
        this.shotInterval = shotInterval;
        try {
            this.image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
            this.image = null;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRango() {
        return rango;
    }

    public void draw(Graphics g) {
        if (image != null) {
            // Ajusta las dimensiones de la imagen al dibujarla
            int width = 80; // Ancho deseado de la imagen de la torre
            int height = 80; // Alto deseado de la imagen de la torre
            g.drawImage(image, x, y, width, height, null);
        }
    }

    public abstract void disparar(List<Enemigo> enemigos);

    public void updateBullets(List<Enemigo> enemigos) {
        Iterator<Bala> itBala = balas.iterator();
        while (itBala.hasNext()) {
            Bala bala = itBala.next();
            bala.mover();
            Iterator<Enemigo> itEnemigo = enemigos.iterator();
            while (itEnemigo.hasNext()) {
                Enemigo enemigo = itEnemigo.next();
                if (enemigo.estaVivo() && bala.impacto(enemigo)) {
                    if (bala.isCongelante()) {
                        enemigo.congelar();
                    } else {
                        enemigo.recibirDaño(bala.getDaño());
                    }
                    itBala.remove();
                    if (!enemigo.estaVivo()) {
                        itEnemigo.remove(); // Remover el enemigo de la lista si ha muerto
                    }
                    break; // Salir del bucle si se ha tratado la bala
                }
            }
        }
    }

    public void drawBullets(Graphics g) {
        for (Bala bala : balas) {
            bala.draw(g);
        }
    }
}

class TorreAtaque extends Torre {
    public TorreAtaque(int x, int y, int shotInterval) {
        super(20, 200, x, y, shotInterval, "src/tower-attack.png");
    }

    @Override
    public void disparar(List<Enemigo> enemigos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime > shotInterval) {
            for (Enemigo enemigo : enemigos) {
                if (Math.hypot(x - enemigo.getX(), y - enemigo.getY()) <= rango && enemigo.estaVivo()) {
                    int dx = (enemigo.getX() - x) / 10;
                    int dy = (enemigo.getY() - y) / 10;
                    balas.add(new Bala(x, y, dx, dy, daño, false, "src/bullet_attack.png"));
                    lastShotTime = currentTime; // Actualizar el tiempo del último disparo
                    break;
                }
            }
        }
    }
}

class TorreCongelacion extends Torre {
    public TorreCongelacion(int x, int y, int shotInterval) {
        super(0, 200, x, y, shotInterval, "src/tower-ice.png");
    }

    @Override
    public void disparar(List<Enemigo> enemigos) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime > shotInterval) {
            for (Enemigo enemigo : enemigos) {
                if (Math.hypot(x - enemigo.getX(), y - enemigo.getY()) <= rango && enemigo.estaVivo()) {
                    int dx = (enemigo.getX() - x) / 10;
                    int dy = (enemigo.getY() - y) / 10;
                    balas.add(new Bala(x, y, dx, dy, 0, true, "src/bullet_ice.png"));
                    lastShotTime = currentTime;
                    break;
                }
            }
        }
    }
}

class Enemigo implements Runnable {
    private int salud;
    private int[][] camino = {
            { 0, 500 }, { 800, 500 }, // Mueve a la derecha completamente
            { 800, 400 },
            { 0, 400 },
            { 0, 300 },
            { 800, 300 }, // Mueve a la izquierda completamente
            { 800, 200 },
            { 0, 200 },
            { 0, 100 },
            { 800, 100 }, // Mueve a la derecha completamente
    };
    private int pasoActual = 0;
    private int x, y;
    private boolean vivo;
    private boolean congelado = false;
    private GamePanel panel; // Referencia al GamePanel
    private BufferedImage image; // Imagen del enemigo

    public Enemigo(int salud, GamePanel panel) {
        this.salud = salud;
        this.x = camino[0][0];
        this.y = camino[0][1];
        this.vivo = true;
        this.panel = panel;
        try {
            this.image = ImageIO.read(new File("src/enemy.png"));
        } catch (IOException e) {
            e.printStackTrace();
            this.image = null;
        }
    }

    public synchronized void recibirDaño(int daño) {
        salud -= daño;
        if (salud <= 0) {
            vivo = false;
            System.out.println("Enemigo derrotado en " + x + ", " + y);
            Thread.currentThread().interrupt(); // Interrumpir el hilo si el enemigo es derrotado
        }
    }

    public void congelar() {
        congelado = true;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean estaVivo() {
        return vivo;
    }

    public int[][] getCamino() {
        return camino;
    }

    public void draw(Graphics g) {
        if (image != null) {
            int adjustedX = x - 50 / 2;
            int adjustedY = y - 50 / 2;
            g.drawImage(image, adjustedX, adjustedY, 50, 50, null); // Dibuja la imagen con tamaño fijo de 30x30
        }
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i < camino.length && vivo; i++) {
                int startX = camino[i - 1][0];
                int startY = camino[i - 1][1];
                int endX = camino[i][0];
                int endY = camino[i][1];
                int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));
                for (int step = 0; step <= steps && vivo; step++) {
                    if (panel.isJuegoActivo() && !congelado && vivo) {
                        x = startX + (endX - startX) * step / steps;
                        y = startY + (endY - startY) * step / steps;
                        if (Math.hypot(x - panel.base.getX(), y - panel.base.getY()) < 100) { // Suponiendo que 50 es el
                                                                                              // rango de detección
                            panel.base.recibirDaño(10); // Daño infligido por el enemigo a la base
                            vivo = false;
                            break;
                        }
                        SwingUtilities.invokeLater(() -> panel.repaint());
                        Thread.sleep(10);
                    } else {
                        return; // Detener el hilo si el juego no está activo
                    }
                    if (congelado) {
                        Thread.sleep(500);
                        congelado = false;
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Hilo de enemigo interrumpido: " + e.getMessage());
            // Limpiar acciones aquí si es necesario
        } finally {
            if (!vivo) {
                System.out.println("El enemigo ha sido eliminado.");
            }
        }
    }
}

class WaveManager {
    private ScheduledExecutorService scheduler;
    private Semaphore semaphore;
    private int waveInterval = 20; // Intervalo entre oleadas en segundos
    private int enemiesPerWave = 5; // Número inicial de enemigos por oleada
    private GamePanel gamePanel; // Referencia a GamePanel

    public WaveManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        scheduler = Executors.newScheduledThreadPool(1);
        semaphore = new Semaphore(1);

    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::launchWave, 0, waveInterval, TimeUnit.SECONDS);
    }

    private void launchWave() {
        if (semaphore.tryAcquire()) {
            try {
                System.out.println("Lanzando oleada con " + enemiesPerWave + " enemigos.");
                for (int i = 0; i < enemiesPerWave; i++) {
                    SwingUtilities.invokeLater(() -> gamePanel.addEnemigo());
                }
                enemiesPerWave++; // Incrementar para la próxima oleada
            } finally {
                semaphore.release();
            }
        } else {
            System.out.println("La oleada anterior aún está activa.");
        }
    }

    public void stop() {
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public void reset() {
        stop();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
}

class GamePanel extends JPanel {
    private List<Torre> torres = new ArrayList<>();
    private List<Enemigo> enemigos = new ArrayList<>();
    public Base base; // La base que los enemigos intentarán destruir
    private BufferedImage grassBackground;
    private BufferedImage pathImage;
    private Timer timer;
    private volatile boolean juegoActivo = true;
    private WaveManager waveManager;

    public GamePanel() {
        setPreferredSize(new Dimension(900, 600));
        try {
            grassBackground = ImageIO.read(new File("src/path_to_grass_with_flowers.jpg"));
            pathImage = ImageIO.read(new File("src/path_to_dirt_path.jpg"));
            base = new Base(100, 750, 50, "src/Base.png", this); // Asumiendo que la base está en esta posición
        } catch (IOException e) {
            e.printStackTrace();
            grassBackground = null;
            pathImage = null;
        }
        timer = new Timer(40, e -> updateGame());
        timer.start();
        waveManager = new WaveManager(this);
        waveManager.start();
    }

    private void updateGame() {
        if (!juegoActivo) {
            timer.stop();
            return;
        }
        // Actualizar las balas y verificar colisiones
        for (Torre torre : torres) {
            torre.updateBullets(enemigos);
            torre.disparar(enemigos); // Asegúrate de que las torres disparen a los enemigos visibles
        }
        repaint();
    }

    public void addTorre(Torre torre) {
        torres.add(torre);
        repaint();
    }

    public void addEnemigo() {
        Enemigo enemigo = new Enemigo(100, this); // Pasa 'this' como referencia al GamePanel
        enemigos.add(enemigo);
        new Thread(enemigo).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Dibujar el fondo de pasto con flores
        if (grassBackground != null) {
            g.drawImage(grassBackground, 0, 0, this.getWidth(), this.getHeight(), this);
        }

        // Dibujar el camino usando la imagen
        if (!enemigos.isEmpty() && pathImage != null) {
            drawPathWithImage(g, enemigos.get(0).getCamino());
        }

        // Dibujar las torres
        for (Torre torre : torres) {
            torre.draw(g);
        }

        for (Torre torre : torres) {
            torre.drawBullets(g);
            torre.updateBullets(enemigos);
        }

        // Dibujar los enemigos
        for (Enemigo enemigo : enemigos) {
            if (enemigo.estaVivo()) {
                enemigo.draw(g);
            }
        }
        if (base != null) {
            base.draw(g); // Dibujar la base
            showBaseHealth(g);
        }
    }
    private void showBaseHealth(Graphics g) {
        // Configuración del texto
        g.setColor(Color.BLACK); // Color del texto
        g.setFont(new Font("Arial", Font.BOLD, 14)); // Fuente del texto
        String healthText = "Vida de la Base: " + base.getSalud();
        g.drawString(healthText, 750, 40); // Dibuja el texto en la posición (750, 40)
    }
    private void drawPathWithImage(Graphics g, int[][] camino) {
        Graphics2D g2d = (Graphics2D) g;
        int pathWidth = 20;
        for (int i = 1; i < camino.length; i++) {
            int x1 = camino[i - 1][0];
            int y1 = camino[i - 1][1];
            int x2 = camino[i][0];
            int y2 = camino[i][1];
            double angle = Math.atan2(y2 - y1, x2 - x1);
            double length = Math.hypot(x2 - x1, y2 - y1);
            int dy = pathWidth / 2;

            g2d.rotate(angle, x1, y1);
            g2d.drawImage(pathImage, x1, y1 - dy, (int) length, pathWidth, null);
            g2d.rotate(-angle, x1, y1);
        }
    }

    public void resetGame() {
        juegoActivo = false;
        timer.stop(); // Detener el timer que actualiza el juego
        waveManager.stop(); // Detener la generación de nuevas oleadas

        // Reinicialización de componentes y estructuras de datos
        enemigos.clear();
        torres.clear();
        base = new Base(1000, 750, 50, "src/Base.png", this);
        juegoActivo = true; // Marcar el juego como activo

        repaint();
        timer.start(); // Reiniciar el timer
        waveManager.reset(); // Reiniciar las oleadas
    }

    public boolean isJuegoActivo() {
        return juegoActivo;
    }

    public void setJuegoActivo(boolean activo) {
        this.juegoActivo = activo;
    }
}

public class TowerDefenseFrame extends JFrame {
    private GamePanel gamePanel = new GamePanel();
    private int[][] posicionesTorres = {
        { 100, 100 }, { 300, 100 }, { 500, 100 }, // Primera fila
        { 100, 200 }, { 300, 200 }, { 500, 200 }, // Segunda fila
        { 100, 300 }, { 300, 300 }, { 500, 300 },
        { 100, 400 }, { 300, 400 }, { 500, 400 }  // Tercera fila
};
private int indicePosicionActual = 0;

    public TowerDefenseFrame() {
        setTitle("Tower Defense Game");
        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);

        JButton spawnEnemyButton = new JButton("Spawn Enemy");
        spawnEnemyButton.addActionListener(e -> gamePanel.addEnemigo()); // Asegúrate de que este método se haya
                                                                         // ajustado adecuadamente

        JButton addAttackTowerButton = new JButton("Add Attack Tower");
        addAttackTowerButton.addActionListener(e -> {
            if (indicePosicionActual < posicionesTorres.length) {
                int[] posicion = posicionesTorres[indicePosicionActual++];
                gamePanel.addTorre(new TorreAtaque(posicion[0], posicion[1], 1000));
            }
        });

        JButton addFreezeTowerButton = new JButton("Add Freeze Tower");
        addFreezeTowerButton.addActionListener(e -> {
            if (indicePosicionActual < posicionesTorres.length) {
                int[] posicion = posicionesTorres[indicePosicionActual++];
                gamePanel.addTorre(new TorreCongelacion(posicion[0], posicion[1], 2000));
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addAttackTowerButton);
        buttonPanel.add(addFreezeTowerButton);
        buttonPanel.add(spawnEnemyButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        new TowerDefenseFrame();
    }
}