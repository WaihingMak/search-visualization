package ecs.visitors;

import java.util.List;

import ai_algorithm.ExploredSet;
import ai_algorithm.Frontier;
import ai_algorithm.Path;
import ai_algorithm.SearchNode;
import ai_algorithm.SearchNodeMetadataObject;
import ai_algorithm.problems.State;
import ai_algorithm.problems.raster_path.GridMazeProblem;
import ai_algorithm.problems.raster_path.GridMazeState;
import ai_algorithm.problems.slidingTilePuzzle.SlidingTileProblem;
import ai_algorithm.problems.slidingTilePuzzle.SlidingTileState;
import ai_algorithm.problems.slidingTilePuzzle.SlidingTileTile;
import application.Globals;
import ecs.Component;
import ecs.GameObject;
import ecs.GameObjectRegistry;
import ecs.components.Animation;
import ecs.components.Association;
import ecs.components.InputConnector;
import ecs.components.InputHandler;
import ecs.components.Position;
import ecs.components.TreeComponent;
import ecs.components.graphics.Coloring;
import ecs.components.graphics.Graphics;
import ecs.components.graphics.TreeLayouter;
import ecs.components.graphics.drawables.ConnectionLine;
import ecs.components.graphics.drawables.Sprite;
import ecs.components.graphics.drawables.Text;
import ecs.components.graphics.drawables.TileMap2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import settings.Settings;
import settings.Settings.TILEMAP;
import tools.Vector2D;
import tools.Vector2DInt;

/**
 * Initializes all game objects that shall be displayed
 */
public class InitialisationVisitor extends Visitor {
	// default
	public void visit(GameObject gameObject) {
		super.visit(gameObject);
		// switch expression mit Pattern matching
		// wie kann das verhindert werden ?
		if (gameObject instanceof SearchNode p) {
			this.visit(p);
			return;
		}
		//
		if (gameObject.getClass().isAssignableFrom(GridMazeProblem.class)) {
			this.visit((GridMazeProblem) gameObject);
			return;
		}
		if (gameObject instanceof GridMazeState girdMazeState) {
			this.visit(girdMazeState);
			return;
		}

		if (gameObject instanceof SlidingTileState slider) {
			this.visit(slider);
			return;
		}

		if (gameObject instanceof SlidingTileTile tile) {
			this.visit(tile);
		}

		if (gameObject instanceof SlidingTileProblem stp) {
			this.visit(stp);
		}

		if (gameObject instanceof Frontier frontier) {
			this.visit(frontier);
			return;
		}

		// Hmmmm..... ka ob das so gut ist ... O.o
		if (gameObject instanceof Path s) {
			if (s.getProblem()instanceof GridMazeProblem prob) {
				this.visit(s, prob);
				return;
			}
		}

		// Platzhalter zum pr�fen
		Component position = new Position(Vector2D.ZERO);
		gameObject.addComponent(position);

		Circle circle = new Circle(0, 0, 20);
		circle.setFill(Color.CHARTREUSE);
		Graphics graphics = new Graphics(Globals.treeRepresentationGraphicsContext);
		gameObject.addComponent(graphics);

		Sprite sprite = new Sprite();
		sprite.addShape(new Circle(100, 100, 100));
		gameObject.addComponent(sprite);
	}

	// SearchNode
	public void visit(SearchNode searchNode) {
		super.visit(searchNode);
		searchNode.addComponent(new Position());
		searchNode.addComponent(new Graphics(Globals.treeRepresentationGraphicsContext));

		Circle circle = new Circle(0, 0, 10);
		circle.setFill(Color.RED);
		Sprite spriteComponent = new Sprite();

		spriteComponent.addShape(circle);
		searchNode.addComponent(spriteComponent);

		Coloring coloring = new Coloring();
		searchNode.addComponent(coloring);

		searchNode.addComponent(new Animation(1));

		Text text = new Text();
		String actionText = searchNode.getAction();
		actionText = actionText == null ? "-" : "" + actionText.charAt(0);
		text.setText("S: " + searchNode.getState().toString() + "\n " + "C: " + searchNode.getPathCost() + "\n A: "
				+ actionText);
		searchNode.addComponent(text);

		if (searchNode.getState().getProblem().isGoalState(searchNode.getState())) {
			coloring.setColor(Color.RED);
		} else {
			coloring.setColor(Color.BLUE);
		}

		if (!searchNode.isRoot()) {
			searchNode.getComponent(Position.class)
					.directSetPosition(searchNode.getParent().getComponent(Position.class).getPosition());
			// assoziation (linie zum parent)
			searchNode.addComponent(new Association(searchNode.getParent()));
			searchNode.addComponent(new ConnectionLine());
			// add treeComponent
			TreeComponent treeComponent = new TreeComponent(searchNode.getParent().getComponent(TreeComponent.class));
			searchNode.addComponent(treeComponent);
			searchNode.getParent().getComponent(TreeLayouter.class).layout();
		} else {
			TreeComponent treeComponent = new TreeComponent();
			searchNode.addComponent(treeComponent);
		}

		TreeLayouter treeLayouter = new TreeLayouter();
		// f�r zweiten baum nach ben wachsen
		if (searchNode.isRoot() && searchNode.metadata.id == 2) {
			treeLayouter.setGrowUp(true);
		}
		searchNode.addComponent(treeLayouter);

		InputHandler ihhover = new InputHandler(e -> {
			// wenn input
			SearchNodeMetadataObject.select(searchNode);
			GameObjectRegistry.registerForStateChange(searchNode);

			if (e != null) {
				e.consume();
			}
			return null;
		});
		searchNode.addComponent(ihhover);
		circle.setOnMouseEntered(InputConnector.getInputConnector(searchNode));

		GameObjectRegistry.registerForStateChange(searchNode);

		searchNode.getComponent(Graphics.class).notifyNewDrawable();
		searchNode.getComponent(Graphics.class).show();

	}

	/**
	 * Initializes Frontier GameObjects
	 * 
	 * @param frontier
	 */
	public void visit(Frontier frontier) {
		super.visit(frontier);
		Graphics g = new Graphics(Globals.treeRepresentationGraphicsContext);
		frontier.addComponent(g);
		var text = frontier.getComponent(Text.class);
		text.setText("Frontier: " + frontier.size());
		text.setFontSize(30);
		frontier.getComponent(Position.class).setPosition(new Vector2D(-100, 0));
		// g.show();
	}

	// #################### GRID_MAZE #######################//

	/**
	 * Initializes GridMazeProblem GameObjects
	 * 
	 * @param frontier
	 */
	public void visit(GridMazeProblem gridMazeProblem) {
		super.visit(gridMazeProblem);

		gridMazeProblem.addComponent(new Graphics(Globals.stateRepresentationGraphicsContext));
		// can cause createt objects that have not been initialized not apera
		// TODO: change how get all works to realy add all objects
		// Fetch all Frontiers
		List<Frontier> frontiers = GameObjectRegistry.getAllGameObjectsOfType(Frontier.class);
		// Fetch all ExploredSets
		List<ExploredSet> exploredSets = GameObjectRegistry.getAllGameObjectsOfType(ExploredSet.class);

		TileMap2D tilemap = new TileMap2D();
		gridMazeProblem.addComponent(tilemap);

		for (int i = 0; i < gridMazeProblem.GAMESIZE; i++) {
			for (int j = 0; j < gridMazeProblem.GAMESIZE; j++) {
				Rectangle r = new Rectangle();

				if (gridMazeProblem.labyrinth[i][j] == 'e') {
					tilemap.insertTile(new Vector2DInt(i, j), r, Color.WHITE);
				} else {
					tilemap.insertTile(new Vector2DInt(i, j), r, Color.gray(0.2));
				}
				final int ic = i;
				final int jc = j;
				r.setOnMouseClicked(e -> {
					if (gridMazeProblem.labyrinth[ic][jc] == 'e') {
						gridMazeProblem.labyrinth[ic][jc] = 'w';
						tilemap.insertTile(new Vector2DInt(ic, jc), r, Color.gray(0.2));
					} else {
						gridMazeProblem.labyrinth[ic][jc] = 'e';
						tilemap.insertTile(new Vector2DInt(ic, jc), r, Color.WHITE);
					}
				});
			}
		}

		Sprite sprites = new Sprite();
		gridMazeProblem.addComponent(sprites);

		Circle goalCircle = new Circle();
		goalCircle.setFill(Color.RED);
		tilemap.fitToTilemap(((GridMazeState) gridMazeProblem.getGoalState()).getPosition(), goalCircle);

		Circle startCircle = new Circle();
		startCircle.setFill(Color.GREEN);
		tilemap.fitToTilemap(((GridMazeState) gridMazeProblem.getInitialState()).getPosition(), startCircle);

		sprites.addShape(goalCircle);
		sprites.addShape(startCircle);

		gridMazeProblem.getComponent(Graphics.class).show();
	}

	/**
	 * Initializes GridMazeState GameObjects
	 * 
	 * @param gridMazeState
	 */
	public void visit(GridMazeState gridMazeState) {
		super.visit(gridMazeState);

		/// KOMMT NICHT AN PARENT STATES RAN UM L�SUNG ANZUZEIGEN MUSS �BER EXTERNE
		/// L�SUNG PASIEREN
		Globals.stateRepresentationGraphicsContext.getChildren().clear();

		Graphics problem = gridMazeState.getProblem().getComponent(Graphics.class);

		problem.show();

		Component position = new Position(Vector2D.ZERO);
		gridMazeState.addComponent(position);

		Circle stateC = new Circle();
		gridMazeState.getProblem().getComponent(TileMap2D.class).fitToTilemap(gridMazeState.getPosition(), stateC);
		stateC.setRadius(4);
		stateC.setFill(Color.CYAN);

		Graphics g = new Graphics(Globals.stateRepresentationGraphicsContext);
		gridMazeState.addComponent(g);

		Sprite sprite = new Sprite();
		gridMazeState.addComponent(sprite);
		sprite.addShape(stateC);

		g.show();

	}

	private void visit(Path path, GridMazeProblem prob) {
		path.addComponent(new Position());
		Graphics g = new Graphics(Globals.stateRepresentationGraphicsContext);
		path.addComponent(g);

		Sprite sprite = new Sprite();
		path.addComponent(sprite);

		List<State> states = path.getVisitedStates();
		List<String> actions = path.getPathActions();

		TileMap2D tileM = new TileMap2D();
		if (prob.hasComponent(TileMap2D.class)) {
			tileM = prob.getComponent(TileMap2D.class);
		}

		if (states.size() >= 2) {
			GridMazeState statePrev = (GridMazeState) states.get(0);
			GridMazeState stateSucc = (GridMazeState) states.get(0);
			for (int i = 1; i < states.size(); i++) {
				statePrev = stateSucc;
				stateSucc = (GridMazeState) states.get(i);

				Vector2D startLine = tileM.fitToTilemap(new Vector2DInt(statePrev.getPosition()), null);
				Vector2D endLine = tileM.fitToTilemap(new Vector2DInt(stateSucc.getPosition()), null);

				Line line = new Line(startLine.x, startLine.y, endLine.x, endLine.y);
				line.setStroke(new Color(1.0, 0, 0, 0.3));
				line.setStyle("-fx-stroke-width: " + 4 + ";");
				sprite.addShape(line);

			}
		}

		Text t = new Text(actions.toString());
		path.addComponent(t);

		g.show();

	}

//###################################### SLIDING TILE ##################################################//

	// aeltere version ohne animation
	public void visit(SlidingTileState slidingTileState) {
		if (slidingTileState.getField() == null) {
			Globals.stateRepresentationGraphicsContext.getChildren().clear();
			return;
		}

		Globals.stateRepresentationGraphicsContext.getChildren().clear();
		Graphics g = new Graphics(Globals.stateRepresentationGraphicsContext);

		slidingTileState.addComponent(g);

		slidingTileState.getComponent(Position.class);
		TileMap2D tilemap = new TileMap2D();
		slidingTileState.addComponent(tilemap);

		int maxval = slidingTileState.getSize().y * slidingTileState.getSize().x;
		for (int x = 0; x < slidingTileState.getSize().x; x++) {
			for (int y = 0; y < slidingTileState.getSize().y; y++) {
				if (slidingTileState.getField()[y][x].getNum() == 0) {
					continue;
				}  
				Rectangle r = new Rectangle();
				double colval = slidingTileState.getField()[y][x].getNum() / (double) maxval;
				colval = Math.min(1.0, Math.max(0.0, colval));
				tilemap.insertTile(new Vector2DInt(x, y), r, Color.gray(colval));
			}
		}

		g.show();
	}


	public void visit(SlidingTileTile stt) {

	}

	public void visit(SlidingTileProblem stt) {

	}

//+++++++++++++++++++++++++++++++++Hatte funktioneirt wieder freigeben nach abgabe+++++++++++++++++++++//
//	public void visit(SlidingTileState slidingTileState) {
//		if (slidingTileState.getField() == null) {
//			// Globals.stateRepresentationGraphicsContext.getChildren().clear();
//			return;
//		}
//
//		// Globals.stateRepresentationGraphicsContext.getChildren().clear();
//		Graphics g = new Graphics(Globals.stateRepresentationGraphicsContext);
//
//		slidingTileState.addComponent(g);
//
////		slidingTileState.getComponent(Position.class);
////		TileMap2D tilemap = new TileMap2D();
////		slidingTileState.addComponent(tilemap);
//
//		int maxval = slidingTileState.getSize().y * slidingTileState.getSize().x;
//		for (int y = 0; y < slidingTileState.getSize().y; y++) {
//			for (int x = 0; x < slidingTileState.getSize().x; x++) {
////				if (slidingTileState.getField()[y][x].getNum() == 0) {
////					continue;
////				}
//				slidingTileState.getField()[y][x].setPos(x, y);
//
//				slidingTileState.getField()[y][x].getComponent(Graphics.class).show();
////				Rectangle r = new Rectangle();
////				double colval = slidingTileState.getField()[y][x].getNum() / (double) maxval;
////				colval = Math.min(1.0, Math.max(0.0, colval));
////				tilemap.insertTile(new Vector2DInt(x, y), r, Color.gray(colval));
//			}
//		}
//	}
//
//	public void visit(SlidingTileTile stt) {
//		super.visit(stt);
//		Graphics g = new Graphics(Globals.stateRepresentationGraphicsContext);
//		stt.addComponent(g);
//		Text text = stt.getComponent(Text.class);
//		text.setText("" + stt.getNum());
//		text.setFontSize(20);
//		stt.getComponent(Position.class).setPosition(stt.getPos().toVector2D().mul(Settings.TILEMAP.TILE_SIZE));
//		stt.getComponent(Animation.class).setDuration(0.1);;
//		
//		Circle c = new Circle(0, 0, TILEMAP.TILE_SIZE);
//		stt.getComponent(Sprite.class).addShape(new Circle(0, 0, TILEMAP.TILE_SIZE,Color.ALICEBLUE));
//		
//		
//		g.show();
//	}
//
//	public void visit(SlidingTileProblem stp) {
//		super.visit(stp);
//
//	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

}

/*
 * Copyright (c) 2022 Severin Dippold
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
