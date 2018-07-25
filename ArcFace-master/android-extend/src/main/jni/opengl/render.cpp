#include <jni.h>

#include <stdio.h>
#include <string.h>

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "Matrix.h"

#include "image.h"
#include "loger.h"
#include "render.h"

typedef struct opengl_t {
	GLuint	m_hProgramObject;
	GLuint	m_nTextureIds[3];
	GLuint	m_nBufs[3];
	GLfloat m_ViewMatrix[16];
	GLfloat m_ModelMatrix[16];
	GLfloat m_Colors[4];
	GLfloat m_pFloatData[8];
    GLfloat m_Vertices[12];
    GLfloat m_Coords[32];
    GLfloat m_CurCoords[8];
    GLfloat m_Scale;
    GLushort m_Indexs[6];
	int m_FloatDataSize;
	int	m_bTexInit;
	int	m_bMirror;
	int	m_nDisplayOrientation;
	int	m_nPixelFormat;
}OPENGLES, *LPOPENGLES;


/**
 *  When an array element i is transferred to the GL by the DrawArrays or DrawElements commands, 
 *  each generic attribute is expanded to four components. If size is one then the x component 
 *  of the attribute is specified by the array; the y, z, and w components are implicitly set to
 *  zero, zero, and one, respectively. If size is two then the x and y components of the attribute
 *  are specified by the array; the z, and w components are implicitly set to zero, and one, 
 *  respectively. If size is three then x, y, and z are specified, and w is implicitly set to one. 
 *  If size is four then all components are specified.
 *  
 */
const char* pVertexShaderStr =
"attribute vec4 a_position;   								\n \
attribute vec2 a_texCoord;   								\n \
varying highp vec2 v_texCoord; 								\n \
void main()                  								\n \
{                            								\n \
	gl_Position = a_position; 								\n \
	v_texCoord = a_texCoord;  								\n \
}                            								\n";


const char* pVertexShaderStrMatrix =
"uniform mat4 u_MMatrix;					       				\n \
uniform mat4 u_VMatrix;						    			\n \
attribute vec4 a_position;							    	\n \
void main() {									    			\n \
	gl_Position = a_position * u_MMatrix * u_VMatrix ;	\n \
}													   			\n";

const char* pVertexShaderSimple =
"attribute vec4 a_position;							    	\n \
void main() {									    			\n \
	gl_Position = a_position ;								\n \
}													   			\n";

const char* pFragmentShaderI420 =
"precision highp float;										\n \
uniform sampler2D y_texture;									\n \
uniform sampler2D uv_texture;								\n \
uniform sampler2D v_texture;								    \n \
varying highp vec2 v_texCoord;								\n \
void main()													\n \
{																\n \
    mediump vec3 yuv;											\n \
    highp vec3 rgb; 											\n \
    yuv.x = texture2D(y_texture, v_texCoord).r;  			\n \
    yuv.y = texture2D(uv_texture, v_texCoord).r;		    \n \
    yuv.z = texture2D(v_texture, v_texCoord).r;		    \n \
    rgb = mat3(      1,       1,       1,					\n \
              0, -0.344, 1.770,								\n \
              1.403, -0.714,       0) * yuv;				\n \
    gl_FragColor = vec4(rgb, 1);								\n \
}																\n";

const char* pFragmentShaderYUYV =
"precision highp float;										\n \
uniform sampler2D y_texture;									\n \
uniform sampler2D uv_texture;								\n \
varying highp vec2 v_texCoord;								\n \
void main()													\n \
{																\n \
    mediump vec3 yuv;											\n \
    highp vec3 rgb; 											\n \
    yuv.x = texture2D(y_texture, v_texCoord).r;  			\n \
    yuv.y = texture2D(uv_texture, v_texCoord).g-0.5;		\n \
    yuv.z = texture2D(uv_texture, v_texCoord).a-0.5;		\n \
    rgb = mat3(      1,       1,       1,					\n \
              0, -0.344, 1.770,								\n \
              1.403, -0.714,       0) * yuv;				\n \
    gl_FragColor = vec4(rgb, 1);								\n \
}																\n";

const char* pFragmentShaderNV21 =
"precision highp float;										\n \
uniform sampler2D y_texture;									\n \
uniform sampler2D uv_texture;								\n \
varying highp vec2 v_texCoord;								\n \
void main()													\n \
{			 													\n \
    mediump vec3 yuv;											\n \
    highp vec3 rgb; 											\n \
    yuv.x = texture2D(y_texture, v_texCoord).r;  			\n \
    yuv.y = texture2D(uv_texture, v_texCoord).a-0.5;		\n \
    yuv.z = texture2D(uv_texture, v_texCoord).r-0.5;		\n \
    rgb = mat3(      1,       1,       1,					\n \
              0, -0.344, 1.770,								\n \
              1.403, -0.714,       0) * yuv;				\n \
    gl_FragColor = vec4(rgb, 1);								\n \
}																\n";

const char* pFragmentShaderNV12 =
"precision highp float; 										\n	\
uniform sampler2D y_texture;									\n \
uniform sampler2D uv_texture;								\n \
varying highp vec2 v_texCoord;								\n \
void main()													\n \
{																\n \
    mediump vec3 yuv;											\n \
    highp vec3 rgb; 											\n \
    yuv.x = texture2D(y_texture, v_texCoord).r;  			\n \
    yuv.y = texture2D(uv_texture, v_texCoord).r-0.5;		\n \
    yuv.z = texture2D(uv_texture, v_texCoord).a-0.5;		\n \
    rgb = mat3(      1,       1,       1,					\n \
              0, -0.344, 1.770,								\n \
              1.403, -0.714,       0) * yuv;				\n \
    gl_FragColor = vec4(rgb, 1);								\n \
}																\n";

const char* pFragmentShaderColor =
"precision mediump float;										\n \
uniform vec4 vColor;											\n \
void main()													\n \
{																\n \
 	gl_FragColor = vColor;									\n \
}																\n";

const char* pFragmentShaderSimple =
"precision mediump float;										\n \
void main()													\n \
{																\n \
	 gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); 				\n \
}																\n";

static GLuint LoadShader(GLenum shaderType, const char* pSource);
static void RotateAndMirror(LPOPENGLES engine);

int GLDrawInit(int mirror, int ori, int format)
{
	LPOPENGLES engine;
	GLuint	vertexShader;
	GLuint	fragmentShader;
	GLint	linked;

	LOGD("GLDrawInit glesInit() <--- format = %d", format);

	engine = (LPOPENGLES)malloc(sizeof(OPENGLES));
	engine->m_hProgramObject		= 0;
	engine->m_bTexInit				= -1;
	engine->m_bMirror				= mirror;
	engine->m_nDisplayOrientation	= ori;
	engine->m_nPixelFormat 			= format;

	vertexShader = LoadShader(GL_VERTEX_SHADER, pVertexShaderStrMatrix); //
	fragmentShader = LoadShader(GL_FRAGMENT_SHADER, pFragmentShaderColor);
	LOGD("GLDrawInit glCreateProgram");
	engine->m_hProgramObject = glCreateProgram();
    if (0 == engine->m_hProgramObject) {
        LOGE("create programObject failed");
        return 0;
    }

	glAttachShader(engine->m_hProgramObject, vertexShader);
	glAttachShader(engine->m_hProgramObject, fragmentShader);

	glBindAttribLocation(engine->m_hProgramObject, 0, "a_position");

	glLinkProgram(engine->m_hProgramObject);

	LOGD("GLDrawInit glLinkProgram");

	glGetProgramiv( engine->m_hProgramObject, GL_LINK_STATUS, &linked);
	LOGD("GLDrawInit glGetProgramiv");
	if (0 == linked) {
		GLint infoLen = 0;
		LOGE("GLDrawInit link failed");
		glGetProgramiv( engine->m_hProgramObject, GL_INFO_LOG_LENGTH, &infoLen);

		if (infoLen > 1) {
			char* infoLog = (char*)malloc(sizeof(char) * infoLen);

			glGetProgramInfoLog( engine->m_hProgramObject, infoLen, NULL, infoLog);
			LOGE( "Error linking program: %s", infoLog);

			free(infoLog);
			infoLog = NULL;
		}

		glDeleteProgram( engine->m_hProgramObject);
        LOGE("GLDrawInit link failed -> out");
		return 0;
	}

	glValidateProgram(engine->m_hProgramObject);
	glGetProgramiv( engine->m_hProgramObject, GL_VALIDATE_STATUS, &linked);
	if (linked == 0) {
		LOGE("program failed");
		return 0;
	}

	glGenBuffers(1, engine->m_nBufs);

	//VBO
	glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[0]);
	glBufferData(GL_ARRAY_BUFFER, sizeof(engine->m_pFloatData), engine->m_pFloatData, GL_STREAM_DRAW);

	//glEnableVertexAttribArray(0);
	//glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(GLfloat), engine->m_pFloatData);

	LOGD("glesInit() --->");
	return (long)engine;
}

int GLImageInit(int mirror, int ori, int format)
{
	LPOPENGLES engine;
	GLuint	vertexShader;
	GLuint	fragmentShader;
	GLint	linked;

	LOGD("GLImageInit glesInit() <--- format = %d", format);

	engine = (LPOPENGLES)malloc(sizeof(OPENGLES));
	engine->m_hProgramObject		= 0;
	engine->m_bTexInit				= -1;
	engine->m_bMirror				= mirror;
	engine->m_nDisplayOrientation	= ori;
	engine->m_nPixelFormat 			= format;

	vertexShader = LoadShader(GL_VERTEX_SHADER, pVertexShaderStr);
	if (engine->m_nPixelFormat == CP_PAF_NV21) {
		fragmentShader = LoadShader(GL_FRAGMENT_SHADER, pFragmentShaderNV21);
	} else if (engine->m_nPixelFormat == CP_PAF_NV12) {
		fragmentShader = LoadShader(GL_FRAGMENT_SHADER, pFragmentShaderNV12);
	} else if (engine->m_nPixelFormat == CP_PAF_YUYV) {
		fragmentShader = LoadShader(GL_FRAGMENT_SHADER, pFragmentShaderYUYV);
	} else if (engine->m_nPixelFormat == CP_PAF_I420) {
        fragmentShader = LoadShader(GL_FRAGMENT_SHADER, pFragmentShaderI420);
    }

	engine->m_hProgramObject = glCreateProgram();
	if (0 == engine->m_hProgramObject) {
		LOGE("create programObject failed");
		return 0;
	}

	LOGD("glAttachShader");

	glAttachShader(engine->m_hProgramObject, vertexShader);
	glAttachShader(engine->m_hProgramObject, fragmentShader);

	LOGD("glBindAttribLocation");
	glBindAttribLocation(engine->m_hProgramObject, 0, "a_position");
	glBindAttribLocation(engine->m_hProgramObject, 1, "a_texCoord");

	glLinkProgram ( engine->m_hProgramObject );

	LOGD("glLinkProgram");

	glGetProgramiv( engine->m_hProgramObject, GL_LINK_STATUS, &linked);
	if (0 == linked) {
		GLint	infoLen = 0;
		LOGE("link failed");
		glGetProgramiv( engine->m_hProgramObject, GL_INFO_LOG_LENGTH, &infoLen);

		if (infoLen > 1) {
			char* infoLog = (char*)malloc(sizeof(char) * infoLen);

			glGetProgramInfoLog( engine->m_hProgramObject, infoLen, NULL, infoLog);
			LOGE( "Error linking program: %s", infoLog);

			free(infoLog);
			infoLog = NULL;
		}

		glDeleteProgram( engine->m_hProgramObject);
        LOGE("link failed -> out");
		return 0;
	}

	//glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	//glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
	//glEnable(GL_TEXTURE_2D);

	LOGD("glGenTextures");
	// Textures
	if (engine->m_nPixelFormat == CP_PAF_NV21 || engine->m_nPixelFormat == CP_PAF_NV12 ||
	    engine->m_nPixelFormat == CP_PAF_YUYV) {
	    glGenTextures(2, engine->m_nTextureIds);
	    glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[1]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	} else if (engine->m_nPixelFormat == CP_PAF_I420) {
	    glGenTextures(3, engine->m_nTextureIds);
	    glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[1]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[2]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	LOGD("VBO");
	//VBO
	glGenBuffers(3, engine->m_nBufs);

    memset(engine->m_Vertices, 0, sizeof(engine->m_Vertices));
    memset(engine->m_Coords, 0, sizeof(engine->m_Coords));

    engine->m_Scale = 1.0;
    engine->m_Vertices[0] = -1.0; engine->m_Vertices[1] = 1.0;  //1.0f, // Position 0
    engine->m_Vertices[3] = -1.0; engine->m_Vertices[4] = -1.0; //1.0f, // Position 1
    engine->m_Vertices[6] = 1.0; engine->m_Vertices[7] = -1.0;  //1.0f, // Position 2
    engine->m_Vertices[9] = 1.0; engine->m_Vertices[10] = 1.0;  //1.0f, // Position 3
    //degree 0
    engine->m_Coords[0] = 0.0; engine->m_Coords[1] = 0.0;
    engine->m_Coords[2] = 0.0; engine->m_Coords[3] = 1.0;
    engine->m_Coords[4] = 1.0; engine->m_Coords[5] = 1.0;
    engine->m_Coords[6] = 1.0; engine->m_Coords[7] = 0.0;
    // degree 90
    engine->m_Coords[8] = engine->m_Coords[2]; engine->m_Coords[9] = engine->m_Coords[3];
    engine->m_Coords[10] = engine->m_Coords[4]; engine->m_Coords[11] = engine->m_Coords[5];
    engine->m_Coords[12] = engine->m_Coords[6]; engine->m_Coords[13] = engine->m_Coords[7];
    engine->m_Coords[14] = engine->m_Coords[0]; engine->m_Coords[15] = engine->m_Coords[1];
    // degree 180
    engine->m_Coords[16] = engine->m_Coords[4]; engine->m_Coords[17] = engine->m_Coords[5];
    engine->m_Coords[18] = engine->m_Coords[6]; engine->m_Coords[19] = engine->m_Coords[7];
    engine->m_Coords[20] = engine->m_Coords[0]; engine->m_Coords[21] = engine->m_Coords[1];
    engine->m_Coords[22] = engine->m_Coords[2]; engine->m_Coords[23] = engine->m_Coords[3];
    // degree 270
    engine->m_Coords[24] = engine->m_Coords[6]; engine->m_Coords[25] = engine->m_Coords[7];
    engine->m_Coords[26] = engine->m_Coords[0]; engine->m_Coords[27] = engine->m_Coords[1];
    engine->m_Coords[28] = engine->m_Coords[2]; engine->m_Coords[29] = engine->m_Coords[3];
    engine->m_Coords[30] = engine->m_Coords[4]; engine->m_Coords[31] = engine->m_Coords[5];

    engine->m_Indexs[0] = 0; engine->m_Indexs[1] = 1; engine->m_Indexs[2] = 2;
    engine->m_Indexs[3] = 0; engine->m_Indexs[4] = 2; engine->m_Indexs[5] = 3;

    RotateAndMirror(engine);

	glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[0]);
	glBufferData(GL_ARRAY_BUFFER, sizeof(engine->m_Vertices), engine->m_Vertices, GL_DYNAMIC_DRAW);
	glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[1]);
	glBufferData(GL_ARRAY_BUFFER, sizeof(engine->m_CurCoords), engine->m_CurCoords, GL_DYNAMIC_DRAW);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, engine->m_nBufs[2]);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(engine->m_Indexs), engine->m_Indexs, GL_STATIC_DRAW);

	LOGD("glesInit() --->");

	return (long)engine;
}

void GLChanged(int handle, int w, int h)
{
	LPOPENGLES engine = (LPOPENGLES)handle;
    LOGD("glesChanged(%d, %d) %x<---", w, h, handle);
    if (engine != NULL) {
        engine->m_bTexInit = -1;
        glViewport(0, 0, w, h);
    }
    LOGD("glesChanged() --->");
}

void GLChangedAngle(int handle, int mirror, int ori)
{
    LPOPENGLES engine = (LPOPENGLES)handle;
    LOGD("GLChangedAngle(%d, %d) <---", mirror, ori);
	if (engine != NULL) {

		engine->m_bMirror = mirror;
		engine->m_nDisplayOrientation = ori;

        RotateAndMirror(engine);

       //glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[0]);
       //glBufferData(GL_ARRAY_BUFFER, sizeof(vVertices), vVertices, GL_STATIC_DRAW);
       //glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[1]);
       //glBufferData(GL_ARRAY_BUFFER, sizeof(tCoords), tCoords, GL_STATIC_DRAW);
	}
    LOGD("GLChangedAngle() --->");
}

void GLDrawRect( int handle, int w, int h, int *point, int rgb, int size)
{
	int i;
	LPOPENGLES engine = (LPOPENGLES)handle;
	if (engine == NULL) {
		LOGE("engine == MNull");
		return;
	}

	/**
	 * left right bottom, top, near, far
	 */
	Matrix::matrixFrustumM(engine->m_ModelMatrix, -0.5f, 0.5f, -0.5f, 0.5f, 1, 10);
	/**
	 * eye x, y, z, 	look x, y, z, 	up x, y ,z
	 */
	Matrix::matrixLookAtM(engine->m_ViewMatrix, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
	Matrix::matrixRotateM(engine->m_ViewMatrix, engine->m_nDisplayOrientation, 0, 0, 1);
	if (engine->m_bMirror == 1) {
		Matrix::matrixScaleM(engine->m_ViewMatrix, -1.0f, 1.0f, 1.0f);
	}

	// use shader
	glUseProgram ( engine->m_hProgramObject );

	engine->m_Colors[0] = (((rgb >> 16) & 0xFF) / 255.0f);
	engine->m_Colors[1] = (((rgb >> 8) & 0xFF) / 255.0f);
	engine->m_Colors[2] = ((rgb & 0xFF) / 255.0f);
	engine->m_Colors[3] = (((rgb >> 24) & 0xFF) / 255.0f);

	GLuint m1 = glGetUniformLocation(engine->m_hProgramObject, "u_VMatrix");
	GLuint m2 = glGetUniformLocation(engine->m_hProgramObject, "u_MMatrix");
	GLuint c1 = glGetUniformLocation(engine->m_hProgramObject, "vColor");

	glUniformMatrix4fv(m1, 1, GL_FALSE, engine->m_ViewMatrix);
	glUniformMatrix4fv(m2, 1, GL_FALSE, engine->m_ModelMatrix);
	glUniform4fv(c1, 1, engine->m_Colors);

	for (i = 0; i < 8; i += 2){
		engine->m_pFloatData[i] = ((2.0f * point[i]) / (w - 1)) - 1.0f;
		engine->m_pFloatData[i + 1] =  1.0f - ((2.0f * point[i + 1]) / (h - 1));
	}

	// update data.
	glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[0]);
	glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(engine->m_pFloatData), engine->m_pFloatData);

	glLineWidth(size);
	glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(GLfloat), 0);
	glEnableVertexAttribArray(0);
	glDrawArrays(GL_LINE_LOOP, 0, 4);
}

void GLImageRender(int handle, unsigned char* pData, int w, int h)
{
	LPOPENGLES engine = (LPOPENGLES)handle;
	if (pData == NULL || engine == NULL) {
		LOGE("GLImageRender FAIL!: 0x%X 0x%X", engine, pData);
		return;
	}
	
	//clean
	glClear ( GL_COLOR_BUFFER_BIT );

	//Texture -> GPU
	if (engine->m_nPixelFormat == CP_PAF_NV21 || engine->m_nPixelFormat == CP_PAF_NV12) {
		glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[0]);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pData);
		glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[1]);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, w >> 1, h >> 1, 0, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, pData + w * h);
	} else if (engine->m_nPixelFormat == CP_PAF_YUYV) {
		glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[0]);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, w, h, 0, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, pData);
		glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[1]);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w >> 1, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, pData);
	} else if (engine->m_nPixelFormat == CP_PAF_I420) {
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pData);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[1]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, w >> 1, h >> 1, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pData + w * h);
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[2]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, w >> 1, h >> 1, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pData + w * h + w * h / 4);
	}
	// use shader
	glUseProgram ( engine->m_hProgramObject );

    GLuint textureUniformY = glGetUniformLocation(engine->m_hProgramObject, "y_texture");
    GLuint textureUniformU = glGetUniformLocation(engine->m_hProgramObject, "uv_texture");

    glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[0]);
    glUniform1i(textureUniformY, 0);

    glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[1]);
    glUniform1i(textureUniformU, 1);

    if (engine->m_nPixelFormat == CP_PAF_I420) {
        GLuint textureUniformV = glGetUniformLocation(engine->m_hProgramObject, "v_texture");
        glBindTexture(GL_TEXTURE_2D, engine->m_nTextureIds[2]);
        glUniform1i(textureUniformV, 2);
    }

	glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[0]);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(engine->m_Vertices), engine->m_Vertices);
	glVertexAttribPointer ( 0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(GLfloat), 0 );
	glEnableVertexAttribArray(0);

	glBindBuffer(GL_ARRAY_BUFFER, engine->m_nBufs[1]);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(engine->m_CurCoords), engine->m_CurCoords);
	glVertexAttribPointer ( 1, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(GLfloat), 0 );
	glEnableVertexAttribArray(1);

	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, engine->m_nBufs[2]);
	glDrawElements ( GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0 );

}

void GLUnInit(int handle)
{
	LPOPENGLES engine = (LPOPENGLES)handle;
    if (engine != NULL) {
        free(engine);
    }
}

void RotateAndMirror(LPOPENGLES engine)
{
    switch(engine->m_nDisplayOrientation) {
        case 0 : memcpy(engine->m_CurCoords, &(engine->m_Coords[0]), sizeof(engine->m_CurCoords)); break;
        case 90 : memcpy(engine->m_CurCoords, &(engine->m_Coords[8]), sizeof(engine->m_CurCoords)); break;
        case 180 : memcpy(engine->m_CurCoords, &(engine->m_Coords[16]), sizeof(engine->m_CurCoords)); break;
        case 270 : memcpy(engine->m_CurCoords, &(engine->m_Coords[24]), sizeof(engine->m_CurCoords)); break;
        default :LOGE("ORIENTATION ERROR! %d\n", engine->m_nDisplayOrientation);
    }

    if (engine->m_nDisplayOrientation == 0 || engine->m_nDisplayOrientation == 180) {
        if (engine->m_bMirror == 1){
            GLfloat temp[2];
            LOGD("set mirror is true");
            temp[0] = engine->m_CurCoords[0]; temp[1] = engine->m_CurCoords[2];
            engine->m_CurCoords[0] = engine->m_CurCoords[4]; engine->m_CurCoords[2] = engine->m_CurCoords[6];
            engine->m_CurCoords[4] = temp[0]; engine->m_CurCoords[6] = temp[1];
        }
    } else {
        if (engine->m_bMirror == 1){
            GLfloat temp[2];
            LOGD("set mirror is true");
            temp[0] = engine->m_CurCoords[1]; temp[1] = engine->m_CurCoords[3];
            engine->m_CurCoords[1] = engine->m_CurCoords[5]; engine->m_CurCoords[3] = engine->m_CurCoords[7];
            engine->m_CurCoords[5] = temp[0]; engine->m_CurCoords[7] = temp[1];
        }
    }
}

GLuint LoadShader(GLenum shaderType, const char* pSource)
{
    GLuint shader = 0;
	shader = glCreateShader(shaderType);
	LOGD("glGetShaderiv called  shader = %d GL_INVALID_ENUM = %d GL_INVALID_OPERATION = %d", shader, GL_INVALID_ENUM, GL_INVALID_OPERATION);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 1;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        LOGD( "glGetShaderiv called compiled = %d, shader = %d", compiled, shader);
        if (!compiled)
		{
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen)
			{
                char* buf = (char*) malloc(infoLen);
                if (buf)
				{
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d: %s",
                            shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
			return 0;
        }
    }
    return shader;
}


