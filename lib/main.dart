import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:pview_delta/models/stroke.dart';
import 'package:flutter_colorpicker/flutter_colorpicker.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Drawing Canvas Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const Scaffold(
        body: DrawingScreen(),
      ),
    );
  }
}

class DrawingScreen extends StatefulWidget {
  const DrawingScreen({super.key});

  @override
  State<DrawingScreen> createState() => _DrawingScreenState();
}

class _DrawingScreenState extends State<DrawingScreen> {
  MethodChannel? _channel;
  bool isPenEnabled = false;
  List<Stroke> strokes = [];
  Size? androidViewSize;
  Color currentColor = Colors.black;
  double currentWidth = 5.0;
  static const double minStrokeWidth = 1.0;
  static const double maxStrokeWidth = 10.0;

  _togglePen() {
    setState(() {
      isPenEnabled = !isPenEnabled;
    });
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        androidViewSize = Size(constraints.maxWidth, constraints.maxHeight);

        return Stack(
          children: [
            CustomPaint(
              painter: ToolsPainter(
                strokes: strokes,
                androidViewSize: androidViewSize,
              ),
              size: const Size(3860, 2160),
            ),
            if (isPenEnabled)
              AndroidView(
                viewType: 'custom_canvas_view',
                creationParams: {
                  'color': currentColor.value,
                  'width': currentWidth,
                },
                creationParamsCodec: const StandardMessageCodec(),
                onPlatformViewCreated: (int id) {
                  print('Flutter: Platform view created with id: $id');
                  _channel = MethodChannel('custom_canvas_view_$id');
                  _channel?.setMethodCallHandler(_handleMethodCall);
                },
              ),
            Positioned(
              bottom: 40,
              right: 200,
              child: Row(
                children: [
                  FloatingActionButton(
                    onPressed: () {
                      showDialog(
                        context: context,
                        builder: (BuildContext context) {
                          return AlertDialog(
                            title: const Text('Pick a color'),
                            content: SingleChildScrollView(
                              child: ColorPicker(
                                pickerColor: currentColor,
                                onColorChanged: (Color color) {
                                  setState(() {
                                    currentColor = color;
                                  });
                                },
                                showLabel: true,
                                pickerAreaHeightPercent: 0.8,
                              ),
                            ),
                            actions: <Widget>[
                              TextButton(
                                child: const Text('Done'),
                                onPressed: () {
                                  _updatePenSettings();
                                  Navigator.of(context).pop();
                                },
                              ),
                            ],
                          );
                        },
                      );
                    },
                    backgroundColor: currentColor,
                    child: const Icon(Icons.color_lens, color: Colors.white),
                  ),
                  const SizedBox(width: 10),
                  Container(
                    width: 200,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(30),
                    ),
                    child: Slider(
                      value: currentWidth,
                      min: minStrokeWidth,
                      max: maxStrokeWidth,
                      divisions: 9,
                      label: currentWidth.round().toString(),
                      onChanged: (double value) {
                        setState(() {
                          currentWidth = value;
                          _updatePenSettings();
                        });
                      },
                    ),
                  ),
                ],
              ),
            ),
            Positioned(
              bottom: 40,
              right: 120,
              child: FloatingActionButton(
                onPressed: _togglePen,
                backgroundColor: isPenEnabled ? Colors.black : Colors.white,
                child: Icon(
                  isPenEnabled ? Icons.edit : Icons.edit_off,
                  color: isPenEnabled ? Colors.red : Colors.red,
                ),
              ),
            ),
            Positioned(
              bottom: 40,
              right: 40,
              child: FloatingActionButton(
                onPressed: () {
                  setState(() {
                    strokes.clear();
                  });
                },
                backgroundColor: Colors.white,
                child: const Icon(
                  Icons.delete_outline,
                  color: Colors.red,
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  void _updatePenSettings() {
    if (_channel != null) {
      // print('Flutter: Updating pen settings - Color: ${currentColor.value}, Width: $currentWidth');
      _channel!.invokeMethod('updatePenSettings', {
        'color': currentColor.value,
        'width': currentWidth,
      }).then((_) {
        // print('Flutter: Pen settings update completed');
      }).catchError((error) {
        print('Flutter: Error updating pen settings: $error');
      });
    } else {
      print('Flutter: Channel is null, cannot update pen settings');
    }
  }

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStrokeComplete':
        try {
          final strokeData = Map<String, dynamic>.from(call.arguments);
          final stroke = Stroke.fromJson(strokeData);
          setState(() {
            strokes.add(Stroke(
              points: stroke.points,
              color: currentColor,
              width: currentWidth,
            ));
          });
          print('Received stroke with ${stroke.points.length} points'); // Debug log
        } catch (e) {
          print('Error processing stroke data: $e'); // Debug log
        }
        break;
    }
  }
}

class ToolsPainter extends CustomPainter {
  final List<Stroke> strokes;
  final Size? androidViewSize;

  ToolsPainter({
    required this.strokes,
    this.androidViewSize,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final stroke in strokes) {
      if (stroke.points.length < 2) continue;

      final paint = Paint()
        ..color = stroke.color
        ..strokeWidth = stroke.width
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..style = PaintingStyle.stroke;

      final path = Path();
      path.moveTo(stroke.points[0].dx, stroke.points[0].dy);

      for (int i = 1; i < stroke.points.length; i++) {
        path.lineTo(stroke.points[i].dx, stroke.points[i].dy);
      }

      canvas.drawPath(path, paint);
    }
  }

  @override
  bool shouldRepaint(ToolsPainter oldDelegate) {
    return true;
  }
}